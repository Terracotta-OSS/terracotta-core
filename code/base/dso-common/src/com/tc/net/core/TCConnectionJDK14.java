/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.bytes.TCByteBuffer;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NIOWorkarounds;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionEventCaller;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.SetOnceRef;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * JDK14 (nio) implementation of TCConnection
 * 
 * @author teck
 */
final class TCConnectionJDK14 implements TCConnection, TCJDK14ChannelReader, TCJDK14ChannelWriter {

  private static final long              NO_CONNECT_TIME     = -1L;
  private static final TCLogger          logger              = TCLogging.getLogger(TCConnection.class);
  private static final long              WARN_THRESHOLD      = 0x400000L;                                       // 4MB

  private final LinkedList               writeContexts       = new LinkedList();
  private final TCCommJDK14              comm;
  private final TCConnectionManagerJDK14 parent;
  private final TCConnectionEventCaller  eventCaller         = new TCConnectionEventCaller(logger);
  private final SynchronizedLong         lastActivityTime    = new SynchronizedLong(System.currentTimeMillis());
  private final SynchronizedLong         connectTime         = new SynchronizedLong(NO_CONNECT_TIME);
  private final List                     eventListeners      = new CopyOnWriteArrayList();
  private final TCProtocolAdaptor        protocolAdaptor;
  private final SynchronizedBoolean      isSocketEndpoint    = new SynchronizedBoolean(false);
  private final SetOnceFlag              closed              = new SetOnceFlag();
  private final SynchronizedBoolean      connected           = new SynchronizedBoolean(false);
  private final SetOnceRef               localSocketAddress  = new SetOnceRef();
  private final SetOnceRef               remoteSocketAddress = new SetOnceRef();
  private volatile SocketChannel         channel;

  // for creating unconnected client connections
  TCConnectionJDK14(TCConnectionEventListener listener, TCCommJDK14 comm, TCProtocolAdaptor adaptor,
                    TCConnectionManagerJDK14 managerJDK14) {
    this(listener, comm, adaptor, null, managerJDK14);
  }

  TCConnectionJDK14(TCConnectionEventListener listener, TCCommJDK14 comm, TCProtocolAdaptor adaptor, SocketChannel ch,
                    TCConnectionManagerJDK14 parent) {
    Assert.assertNotNull(parent);
    Assert.assertNotNull(adaptor);

    this.parent = parent;
    this.protocolAdaptor = adaptor;

    if (listener != null) addListener(listener);

    Assert.assertNotNull(comm);
    this.comm = comm;
    this.channel = ch;
  }

  private void closeImpl(Runnable callback) {
    Assert.assertTrue(closed.isSet());
    try {
      if (channel != null) {
        comm.cleanupChannel(channel, callback);
      } else {
        callback.run();
      }
    } finally {
      synchronized (writeContexts) {
        writeContexts.clear();
      }
    }
  }

  protected void finishConnect() {
    Assert.assertNotNull("channel", channel);
    recordSocketAddress(channel.socket());
    setConnected(true);
    eventCaller.fireConnectEvent(eventListeners, this);
  }

  private void connectImpl(TCSocketAddress addr, int timeout) throws IOException, TCTimeoutException {
    SocketChannel newSocket = null;
    InetSocketAddress inetAddr = new InetSocketAddress(addr.getAddress(), addr.getPort());
    for (int i = 1; i <= 3; i++) {
      try {
        newSocket = createChannel();
        newSocket.configureBlocking(true);
        newSocket.socket().connect(inetAddr, timeout);
        break;
      } catch (SocketTimeoutException ste) {
        comm.cleanupChannel(newSocket, null);
        throw new TCTimeoutException("Timeout of " + timeout + "ms occured connecting to " + addr, ste);
      } catch (ClosedSelectorException cse) {
        if (NIOWorkarounds.windowsConnectWorkaround(cse)) {
          logger.warn("Retrying connect to " + addr + ", attempt " + i);
          ThreadUtil.reallySleep(500);
          continue;
        }
        throw cse;
      }
    }

    channel = newSocket;
    newSocket.configureBlocking(false);
    comm.requestReadInterest(this, newSocket);
  }

  private SocketChannel createChannel() throws IOException, SocketException {
    SocketChannel rv = SocketChannel.open();
    Socket s = rv.socket();

    // TODO: provide config options for setting any and all socket options
    s.setSendBufferSize(64 * 1024);
    s.setReceiveBufferSize(64 * 1024);
    // s.setReuseAddress(true);
    s.setTcpNoDelay(true);

    return rv;
  }

  private Socket detachImpl() throws IOException {
    comm.unregister(channel);
    channel.configureBlocking(true);
    return channel.socket();
  }

  private boolean asynchConnectImpl(TCSocketAddress address) throws IOException {
    SocketChannel newSocket = createChannel();
    newSocket.configureBlocking(false);

    InetSocketAddress inetAddr = new InetSocketAddress(address.getAddress(), address.getPort());
    final boolean rv = newSocket.connect(inetAddr);
    setConnected(rv);

    channel = newSocket;

    if (!rv) {
      comm.requestConnectInterest(this, newSocket);
    }

    return rv;
  }

  public void doRead(ScatteringByteChannel sbc) {
    final boolean debug = logger.isDebugEnabled();
    final TCByteBuffer[] readBuffers = getReadBuffers();

    int bytesRead = 0;
    boolean readEOF = false;
    try {
      // Do the read in a loop, instead of calling read(ByteBuffer[]).
      // This seems to avoid memory leaks on sun's 1.4.2 JDK
      for (int i = 0, n = readBuffers.length; i < n; i++) {
        ByteBuffer buf = extractNioBuffer(readBuffers[i]);

        if (buf.hasRemaining()) {
          final int read = sbc.read(buf);

          if (-1 == read) {
            // Normal EOF
            readEOF = true;
            break;
          }

          if (0 == read) {
            break;
          }

          bytesRead += read;

          if (buf.hasRemaining()) {
            // don't move on to the next buffer if we didn't fill the current one
            break;
          }
        }
      }
    } catch (IOException ioe) {
      if (logger.isInfoEnabled()) {
        logger.info("error reading from channel " + channel.toString() + ": " + ioe.getMessage());
      }

      eventCaller.fireErrorEvent(eventListeners, this, ioe, null);
      return;
    }

    if (readEOF) {
      if (bytesRead > 0) {
        addNetworkData(readBuffers, bytesRead);
      }

      if (debug) logger.debug("EOF read on connection " + channel.toString());

      eventCaller.fireEndOfFileEvent(eventListeners, this);
      return;
    }

    Assert.eval(bytesRead >= 0);

    if (debug) logger.debug("Read " + bytesRead + " bytes on connection " + channel.toString());

    addNetworkData(readBuffers, bytesRead);
  }

  public void doWrite(GatheringByteChannel gbc) {
    final boolean debug = logger.isDebugEnabled();

    // get a copy of the current write contexts. Since we call out to event/error handlers in the write
    // loop below, we don't want to be holding the lock on the writeContexts queue
    final WriteContext contextsToWrite[];
    synchronized (writeContexts) {
      if (closed.isSet()) { return; }
      contextsToWrite = (WriteContext[]) writeContexts.toArray(new WriteContext[writeContexts.size()]);
    }

    int contextsToRemove = 0;
    for (int index = 0, n = contextsToWrite.length; index < n; index++) {
      final WriteContext context = contextsToWrite[index];
      final ByteBuffer[] buffers = context.clonedData;

      long bytesWritten = 0;
      try {
        // Do the write in a loop, instead of calling write(ByteBuffer[]).
        // This seems to avoid memory leaks on sun's 1.4.2 JDK
        for (int i = context.index, nn = buffers.length; i < nn; i++) {
          final ByteBuffer buf = buffers[i];
          final int written = gbc.write(buf);

          if (written == 0) {
            break;
          }

          bytesWritten += written;

          if (buf.hasRemaining()) {
            break;
          } else {
            context.incrementIndex();
          }
        }
      } catch (IOException ioe) {
        if (NIOWorkarounds.windowsWritevWorkaround(ioe)) {
          break;
        }

        eventCaller.fireErrorEvent(eventListeners, this, ioe, context.message);
      }

      if (debug) logger.debug("Wrote " + bytesWritten + " bytes on connection " + channel.toString());

      if (context.done()) {
        contextsToRemove++;
        if (debug) logger.debug("Complete message sent on connection " + channel.toString());
        context.writeComplete();
      } else {
        if (debug) logger.debug("Message not yet completely sent on connection " + channel.toString());
        break;
      }
    }

    synchronized (writeContexts) {
      if (closed.isSet()) { return; }

      for (int i = 0; i < contextsToRemove; i++) {
        writeContexts.removeFirst();
      }

      if (writeContexts.isEmpty()) {
        comm.removeWriteInterest(this, channel);
      }
    }
  }

  static private long bytesRemaining(ByteBuffer[] buffers) {
    long rv = 0;
    for (int i = 0, n = buffers.length; i < n; i++) {
      rv += buffers[i].remaining();
    }
    return rv;
  }

  static private ByteBuffer[] extractNioBuffers(TCByteBuffer[] src) {
    ByteBuffer[] rv = new ByteBuffer[src.length];
    for (int i = 0, n = src.length; i < n; i++) {
      rv[i] = src[i].getNioBuffer();
    }

    return rv;
  }

  static private ByteBuffer extractNioBuffer(TCByteBuffer buffer) {
    return buffer.getNioBuffer();
  }

  private void putMessageImpl(TCNetworkMessage message) {
    // ??? Does the message queue and the WriteContext belong in the base connection class?
    final boolean debug = logger.isDebugEnabled();

    final WriteContext context = new WriteContext(message);

    final long bytesToWrite = bytesRemaining(context.clonedData);
    if (bytesToWrite >= TCConnectionJDK14.WARN_THRESHOLD) {
      logger.warn("Warning: Attempting to send a messaage of size " + bytesToWrite + " bytes");
    }

    // TODO: outgoing queue should not be unbounded size!
    final boolean newData;
    final int msgCount;
    synchronized (writeContexts) {
      if (closed.isSet()) { return; }

      writeContexts.addLast(context);
      msgCount = writeContexts.size();
      newData = (msgCount == 1);
    }

    if (debug) {
      logger.debug("Connection (" + channel.toString() + ") has " + msgCount + " messages queued");
    }

    if (newData) {
      if (debug) {
        logger.debug("New message on connection, registering for write interest");
      }

      // NOTE: this might be the very first message on the socket and
      // given the current implementation, it isn't necessarily
      // safe to assume one can write to the channel. Long story
      // short, always enqueue the message and wait until it is selected
      // for write interest.

      // If you're trying to optimize for performance by letting the calling thread do the
      // write, we need to add more logic to connection setup. Specifically, you need register
      // for, as well as actually be selected for, write interest immediately
      // after finishConnect(). Only after this selection occurs it is always safe to try
      // to write.

      comm.requestWriteInterest(this, channel);
    }
  }

  public final void asynchClose() {
    if (closed.attemptSet()) {
      closeImpl(createCloseCallback(null));
    }
  }

  public final boolean close(final long timeout) {
    if (timeout <= 0) { throw new IllegalArgumentException("timeout cannot be less than or equal to zero"); }

    if (closed.attemptSet()) {
      final Latch latch = new Latch();
      closeImpl(createCloseCallback(latch));
      try {
        return latch.attempt(timeout);
      } catch (InterruptedException e) {
        logger.warn("close interrupted");
        return isConnected();
      }
    }

    return isClosed();
  }

  private final Runnable createCloseCallback(final Latch latch) {
    final boolean fireClose = isConnected();

    return new Runnable() {
      public void run() {
        setConnected(false);
        parent.connectionClosed(TCConnectionJDK14.this);

        if (fireClose) {
          eventCaller.fireCloseEvent(eventListeners, TCConnectionJDK14.this);
        }

        if (latch != null) latch.release();
      }
    };
  }

  public final boolean isClosed() {
    return closed.isSet();
  }

  public final boolean isConnected() {
    return connected.get();
  }

  public final String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append(getClass().getName()).append('@').append(hashCode()).append(":");

    buf.append(" connected: ").append(isConnected());
    buf.append(", closed: ").append(isClosed());

    if (isSocketEndpoint.get()) {
      buf.append(" local=");
      if (localSocketAddress.isSet()) {
        buf.append(((TCSocketAddress) localSocketAddress.get()).getStringForm());
      } else {
        buf.append("[unknown]");
      }

      buf.append(" remote=");
      if (remoteSocketAddress.isSet()) {
        buf.append(((TCSocketAddress) remoteSocketAddress.get()).getStringForm());
      } else {
        buf.append("[unknown");
      }
    }

    buf.append(" connect=[");
    final long connect = getConnectTime();

    if (connect != NO_CONNECT_TIME) {
      buf.append(new Date(connect));
    } else {
      buf.append("no connect time");
    }
    buf.append(']');

    buf.append(" idle=").append(getIdleTime()).append("ms");

    return buf.toString();
  }

  public final void addListener(TCConnectionEventListener listener) {
    if (listener == null) { return; }
    eventListeners.add(listener); // don't need sync
  }

  public final void removeListener(TCConnectionEventListener listener) {
    if (listener == null) { return; }
    eventListeners.remove(listener); // don't need sync
  }

  public final long getConnectTime() {
    return connectTime.get();
  }

  public final long getIdleTime() {
    return System.currentTimeMillis() - lastActivityTime.get();
  }

  public final synchronized void connect(TCSocketAddress addr, int timeout) throws IOException, TCTimeoutException {
    if (closed.isSet() || connected.get()) { throw new IllegalStateException("Connection closed or already connected"); }
    connectImpl(addr, timeout);
    finishConnect();
  }

  public final synchronized boolean asynchConnect(TCSocketAddress addr) throws IOException {
    if (closed.isSet() || connected.get()) { throw new IllegalStateException("Connection closed or already connected"); }

    boolean rv = asynchConnectImpl(addr);

    if (rv) {
      finishConnect();
    }

    return rv;
  }

  public final void putMessage(TCNetworkMessage message) {
    lastActivityTime.set(System.currentTimeMillis());

    // if (!isConnected() || isClosed()) {
    // logger.warn("Ignoring message sent to non-connected connection");
    // return;
    // }

    putMessageImpl(message);
  }

  public final TCSocketAddress getLocalAddress() {
    return (TCSocketAddress) localSocketAddress.get();
  }

  public final TCSocketAddress getRemoteAddress() {
    return (TCSocketAddress) remoteSocketAddress.get();
  }

  private final void setConnected(boolean connected) {
    if (connected) {
      this.connectTime.set(System.currentTimeMillis());
    }
    this.connected.set(connected);
  }

  private final void recordSocketAddress(Socket socket) {
    if (socket != null) {
      isSocketEndpoint.set(true);
      localSocketAddress.set(new TCSocketAddress(socket.getLocalAddress(), socket.getLocalPort()));
      remoteSocketAddress.set(new TCSocketAddress(socket.getInetAddress(), socket.getPort()));
    }
  }

  private final void addNetworkData(TCByteBuffer[] data, int length) {
    lastActivityTime.set(System.currentTimeMillis());

    try {
      protocolAdaptor.addReadData(this, data, length);
    } catch (Exception e) {
      eventCaller.fireErrorEvent(eventListeners, this, e, null);
      return;
    }
  }

  protected final TCByteBuffer[] getReadBuffers() {
    // TODO: Hook in some form of read throttle. To throttle how much data is read from the network,
    // only return a subset of the buffers that the protocolAdaptor advises to be used.

    // TODO: should also support a way to de-register read interest temporarily

    return protocolAdaptor.getReadBuffers();
  }

  protected final void fireErrorEvent(Exception e, TCNetworkMessage context) {
    eventCaller.fireErrorEvent(eventListeners, this, e, context);
  }

  public final Socket detach() throws IOException {
    this.parent.removeConnection(this);
    return detachImpl();
  }

  private static class WriteContext {
    private final TCNetworkMessage message;
    private final ByteBuffer[]     clonedData;
    private int                    index = 0;

    WriteContext(TCNetworkMessage message) {
      this.message = message;

      final ByteBuffer[] msgData = extractNioBuffers(message.getEntireMessageData());
      this.clonedData = new ByteBuffer[msgData.length];

      for (int i = 0; i < msgData.length; i++) {
        clonedData[i] = msgData[i].duplicate().asReadOnlyBuffer();
      }
    }

    boolean done() {
      for (int i = index, n = clonedData.length; i < n; i++) {
        if (clonedData[i].hasRemaining()) { return false; }
      }

      return true;
    }

    void incrementIndex() {
      clonedData[index] = null;
      index++;
    }

    void writeComplete() {
      this.message.wasSent();
    }
  }

}
