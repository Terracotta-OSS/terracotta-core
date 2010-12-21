/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NIOWorkarounds;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionEventCaller;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.transport.WireProtocolGroupMessageImpl;
import com.tc.net.protocol.transport.WireProtocolHeader;
import com.tc.net.protocol.transport.WireProtocolMessage;
import com.tc.net.protocol.transport.WireProtocolMessageImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.SetOnceRef;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The {@link TCConnection} implementation. SocketChannel read/write happens here.
 * 
 * @author teck
 * @author mgovinda
 */
final class TCConnectionImpl implements TCConnection, TCChannelReader, TCChannelWriter {

  private static final long                  NO_CONNECT_TIME             = -1L;
  private static final TCLogger              logger                      = TCLogging.getLogger(TCConnection.class);
  private static final long                  WARN_THRESHOLD              = 0x400000L;                                                    // 4MB

  private volatile CoreNIOServices           commWorker;
  private volatile SocketChannel             channel;

  private final AtomicBoolean                transportEstablished        = new AtomicBoolean(false);
  private final LinkedList<TCNetworkMessage> writeMessages               = new LinkedList<TCNetworkMessage>();
  private final TCConnectionManagerImpl      parent;
  private final TCConnectionEventCaller      eventCaller                 = new TCConnectionEventCaller(logger);
  private final SynchronizedLong             lastDataWriteTime           = new SynchronizedLong(
                                                                                                System
                                                                                                    .currentTimeMillis());
  private final SynchronizedLong             lastDataReceiveTime         = new SynchronizedLong(
                                                                                                System
                                                                                                    .currentTimeMillis());
  private final SynchronizedLong             connectTime                 = new SynchronizedLong(NO_CONNECT_TIME);
  private final List                         eventListeners              = new CopyOnWriteArrayList();
  private final TCProtocolAdaptor            protocolAdaptor;
  private final SynchronizedBoolean          isSocketEndpoint            = new SynchronizedBoolean(false);
  private final SetOnceFlag                  closed                      = new SetOnceFlag();
  private final SynchronizedBoolean          connected                   = new SynchronizedBoolean(false);
  private final SetOnceRef                   localSocketAddress          = new SetOnceRef();
  private final SetOnceRef                   remoteSocketAddress         = new SetOnceRef();
  private final SocketParams                 socketParams;
  private final SynchronizedLong             totalRead                   = new SynchronizedLong(0);
  private final SynchronizedLong             totalWrite                  = new SynchronizedLong(0);
  private final ArrayList<WriteContext>      writeContexts               = new ArrayList<WriteContext>();

  private static final boolean               MSG_GROUPING_ENABLED        = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getBoolean(TCPropertiesConsts.TC_MESSAGE_GROUPING_ENABLED);
  private static final int                   MSG_GROUPING_MAX_SIZE_BYTES = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getInt(TCPropertiesConsts.TC_MESSAGE_GROUPING_MAXSIZE_KB,
                                                                                     128) * 1024;
  private static final boolean               MESSSAGE_PACKUP             = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getBoolean(TCPropertiesConsts.TC_MESSAGE_PACKUP_ENABLED,
                                                                                         true);

  static {
    logger.info("Comms Message Batching " + (MSG_GROUPING_ENABLED ? "enabled" : "disabled"));
  }

  // having this variable at instance level helps reducing memory pressure at VM;
  private final ArrayList<TCNetworkMessage>  messagesToBatch             = new ArrayList<TCNetworkMessage>();

  // for creating unconnected client connections
  TCConnectionImpl(final TCConnectionEventListener listener, final TCProtocolAdaptor adaptor,
                   final TCConnectionManagerImpl managerJDK14, final CoreNIOServices nioServiceThread,
                   final SocketParams socketParams) {
    this(listener, adaptor, null, managerJDK14, nioServiceThread, socketParams);
  }

  TCConnectionImpl(final TCConnectionEventListener listener, final TCProtocolAdaptor adaptor, final SocketChannel ch,
                   final TCConnectionManagerImpl parent, final CoreNIOServices nioServiceThread,
                   final SocketParams socketParams) {

    Assert.assertNotNull(parent);
    Assert.assertNotNull(adaptor);

    this.parent = parent;
    this.protocolAdaptor = adaptor;

    if (listener != null) {
      addListener(listener);
    }

    this.channel = ch;

    if (ch != null) {
      socketParams.applySocketParams(ch.socket());
    }

    this.socketParams = socketParams;
    this.commWorker = nioServiceThread;
  }

  public void setCommWorker(final CoreNIOServices worker) {
    this.commWorker = worker;
  }

  private void closeImpl(final Runnable callback) {
    Assert.assertTrue(this.closed.isSet());
    this.transportEstablished.set(false);
    try {
      if (this.channel != null) {
        this.commWorker.cleanupChannel(this.channel, callback);
      } else {
        callback.run();
      }
    } finally {
      synchronized (this.writeMessages) {
        this.writeMessages.clear();
      }
    }
  }

  protected void finishConnect() throws IOException {
    Assert.assertNotNull("channel", this.channel);
    recordSocketAddress(this.channel.socket());
    setConnected(true);
    this.eventCaller.fireConnectEvent(this.eventListeners, this);
  }

  private void connectImpl(final TCSocketAddress addr, final int timeout) throws IOException, TCTimeoutException {
    SocketChannel newSocket = null;
    final InetSocketAddress inetAddr = new InetSocketAddress(addr.getAddress(), addr.getPort());
    for (int i = 1; i <= 3; i++) {
      try {
        newSocket = createChannel();
        newSocket.configureBlocking(true);
        newSocket.socket().connect(inetAddr, timeout);
        break;
      } catch (final SocketTimeoutException ste) {
        Assert.eval(this.commWorker != null);
        this.commWorker.cleanupChannel(newSocket, null);
        throw new TCTimeoutException("Timeout of " + timeout + "ms occured connecting to " + addr, ste);
      } catch (final ClosedSelectorException cse) {
        if (NIOWorkarounds.connectWorkaround(cse)) {
          logger.warn("Retrying connect to " + addr + ", attempt " + i);
          ThreadUtil.reallySleep(500);
          continue;
        }
        throw cse;
      }
    }

    this.channel = newSocket;
    newSocket.configureBlocking(false);
    Assert.eval(this.commWorker != null);
    this.commWorker.requestReadInterest(this, newSocket);
  }

  private SocketChannel createChannel() throws IOException, SocketException {
    final SocketChannel rv = SocketChannel.open();
    final Socket s = rv.socket();
    this.socketParams.applySocketParams(s);
    return rv;
  }

  private Socket detachImpl() throws IOException {
    this.commWorker.detach(this.channel);
    this.channel.configureBlocking(true);
    return this.channel.socket();
  }

  private boolean asynchConnectImpl(final TCSocketAddress address) throws IOException {
    final SocketChannel newSocket = createChannel();
    newSocket.configureBlocking(false);

    final InetSocketAddress inetAddr = new InetSocketAddress(address.getAddress(), address.getPort());
    final boolean rv = newSocket.connect(inetAddr);
    setConnected(rv);

    this.channel = newSocket;

    if (!rv) {
      this.commWorker.requestConnectInterest(this, newSocket);
    }

    return rv;
  }

  public int doRead(final ScatteringByteChannel sbc) {
    final int read = doReadInternal(sbc);
    this.totalRead.add(read);
    return read;
  }

  public int doWrite(final GatheringByteChannel gbc) {
    final int written = doWriteInternal(gbc);
    this.totalWrite.add(written);
    return written;
  }

  private void buildWriteContextsFromMessages() {
    TCNetworkMessage messagesToWrite[];
    synchronized (this.writeMessages) {
      if (this.closed.isSet()) { return; }
      messagesToWrite = this.writeMessages.toArray(new TCNetworkMessage[this.writeMessages.size()]);
      this.writeMessages.clear();
    }

    int batchSize = 0;
    int batchMsgCount = 0;
    TCNetworkMessage msg = null;
    for (final TCNetworkMessage element : messagesToWrite) {
      msg = element;

      // we don't want to group already constructed Transport Handshake WireProtocolMessages
      if (msg instanceof WireProtocolMessage) {
        final TCNetworkMessage ms = finalizeWireProtocolMessage((WireProtocolMessage) msg, 1);
        this.writeContexts.add(new WriteContext(ms));
        continue;
      }

      // GenericNetwork messages are used for testing
      if (WireProtocolHeader.PROTOCOL_UNKNOWN == WireProtocolHeader.getProtocolForMessageClass(msg)) {
        this.writeContexts.add(new WriteContext(msg));
        continue;
      }

      if (MSG_GROUPING_ENABLED) {
        if (!canBatch(msg, batchSize, batchMsgCount)) {
          if (batchMsgCount > 0) {
            this.writeContexts.add(new WriteContext(buildWireProtocolMessageGroup(this.messagesToBatch)));
            batchSize = 0;
            batchMsgCount = 0;
            this.messagesToBatch.clear();
          } else {
            // fall thru and add to the existing batch. next message will goto a new batch
          }
        }
        batchSize += getRealMessgeSize(msg.getTotalLength());
        batchMsgCount++;
        this.messagesToBatch.add(msg);
      } else {
        this.writeContexts.add(new WriteContext(buildWireProtocolMessage(msg)));
      }
      msg = null;
    }

    if (MSG_GROUPING_ENABLED && batchMsgCount > 0) {
      final TCNetworkMessage ms = buildWireProtocolMessageGroup(this.messagesToBatch);
      this.writeContexts.add(new WriteContext(ms));
    }

    messagesToWrite = null;
    this.messagesToBatch.clear();
  }

  private boolean canBatch(final TCNetworkMessage newMessage, final int currentBatchSize, final int currentBatchMsgCount) {
    if ((currentBatchSize + getRealMessgeSize(newMessage.getTotalLength())) <= MSG_GROUPING_MAX_SIZE_BYTES
        && (currentBatchMsgCount + 1 <= WireProtocolHeader.MAX_MESSAGE_COUNT)) { return true; }
    return false;
  }

  private int getRealMessgeSize(final int length) {
    return TCByteBufferFactory.getTotalBufferSizeNeededForMessageSize(length);
  }

  private int doReadInternal(final ScatteringByteChannel sbc) {
    final boolean debug = logger.isDebugEnabled();
    final TCByteBuffer[] readBuffers = getReadBuffers();

    int bytesRead = 0;
    boolean readEOF = false;
    try {
      // Do the read in a loop, instead of calling read(ByteBuffer[]).
      // This seems to avoid memory leaks on sun's 1.4.2 JDK
      for (final TCByteBuffer readBuffer : readBuffers) {
        final ByteBuffer buf = extractNioBuffer(readBuffer);

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
    } catch (final IOException ioe) {
      if (logger.isInfoEnabled()) {
        logger.info("error reading from channel " + this.channel.toString() + ": " + ioe.getMessage());
      }

      this.eventCaller.fireErrorEvent(this.eventListeners, this, ioe, null);
      return bytesRead;
    }

    if (readEOF) {
      if (bytesRead > 0) {
        addNetworkData(readBuffers, bytesRead);
      }

      if (debug) {
        logger.debug("EOF read on connection " + this.channel.toString());
      }

      this.eventCaller.fireEndOfFileEvent(this.eventListeners, this);
      return bytesRead;
    }

    Assert.eval(bytesRead >= 0);

    if (debug) {
      logger.debug("Read " + bytesRead + " bytes on connection " + this.channel.toString());
    }

    addNetworkData(readBuffers, bytesRead);

    return bytesRead;
  }

  public int doWriteInternal(final GatheringByteChannel gbc) {
    final boolean debug = logger.isDebugEnabled();
    int totalBytesWritten = 0;

    // get a copy of the current write contexts. Since we call out to event/error handlers in the write
    // loop below, we don't want to be holding the lock on the writeContexts queue
    if (this.writeContexts.size() <= 0) {
      buildWriteContextsFromMessages();
    }
    WriteContext context;
    while (this.writeContexts.size() > 0) {
      context = this.writeContexts.get(0);
      final TCByteBuffer[] buffers = context.entireMessageData;

      long bytesWritten = 0;
      try {
        // Do the write in a loop, instead of calling write(ByteBuffer[]).
        // This seems to avoid memory leaks and faster
        for (int i = context.index, nn = buffers.length; i < nn; i++) {
          final int written = gbc.write(buffers[i].getNioBuffer());
          if (written == 0) {
            break;
          }

          bytesWritten += written;

          if (buffers[i].hasRemaining()) {
            break;
          } else {
            context.incrementIndexAndCleanOld();
          }
        }
      } catch (final IOException ioe) {
        if (NIOWorkarounds.windowsWritevWorkaround(ioe)) {
          break;
        }

        this.eventCaller.fireErrorEvent(this.eventListeners, this, ioe, context.message);
      }

      if (debug) {
        logger.debug("Wrote " + bytesWritten + " bytes on connection " + this.channel.toString());
      }
      totalBytesWritten += bytesWritten;

      if (context.done()) {
        if (debug) {
          logger.debug("Complete message sent on connection " + this.channel.toString());
        }
        context.writeComplete();
        this.writeContexts.remove(context);
      } else {
        if (debug) {
          logger.debug("Message not yet completely sent on connection " + this.channel.toString());
        }
        break;
      }
    }

    synchronized (this.writeMessages) {
      if (this.closed.isSet()) { return totalBytesWritten; }

      if (this.writeMessages.isEmpty() && this.writeContexts.isEmpty()) {
        this.commWorker.removeWriteInterest(this, this.channel);
      }
    }
    return totalBytesWritten;
  }

  static private ByteBuffer extractNioBuffer(final TCByteBuffer buffer) {
    return buffer.getNioBuffer();
  }

  private void putMessageImpl(final TCNetworkMessage message) {
    // ??? Does the message queue and the WriteContext belong in the base connection class?
    final boolean debug = logger.isDebugEnabled();

    long bytesToWrite = 0;
    bytesToWrite = message.getTotalLength();
    if (bytesToWrite >= TCConnectionImpl.WARN_THRESHOLD) {
      logger.warn("Warning: Attempting to send a message (" + message.getClass().getName() + ") of size "
                  + bytesToWrite + " bytes");
    }

    // TODO: outgoing queue should not be unbounded size!
    final boolean newData;
    final int msgCount;

    synchronized (this.writeMessages) {
      if (this.closed.isSet()) { return; }
      this.writeMessages.addLast(message);
      msgCount = this.writeMessages.size();
      newData = (msgCount == 1);
    }

    if (debug) {
      logger.debug("Connection (" + this.channel.toString() + ") has " + msgCount + " messages queued");
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
      this.commWorker.requestWriteInterest(this, this.channel);
    }
  }

  public final void asynchClose() {
    if (this.closed.attemptSet()) {
      closeImpl(createCloseCallback(null));
    }
  }

  public final boolean close(final long timeout) {
    if (timeout <= 0) { throw new IllegalArgumentException("timeout cannot be less than or equal to zero"); }

    if (this.closed.attemptSet()) {
      final Latch latch = new Latch();
      closeImpl(createCloseCallback(latch));
      try {
        return latch.attempt(timeout);
      } catch (final InterruptedException e) {
        logger.warn("close interrupted");
        Thread.currentThread().interrupt();
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
        TCConnectionImpl.this.parent.connectionClosed(TCConnectionImpl.this);

        if (fireClose) {
          TCConnectionImpl.this.eventCaller.fireCloseEvent(TCConnectionImpl.this.eventListeners, TCConnectionImpl.this);
        }

        if (latch != null) {
          latch.release();
        }
      }
    };
  }

  public final boolean isClosed() {
    return this.closed.isSet();
  }

  public final boolean isConnected() {
    return this.connected.get();
  }

  @Override
  public final String toString() {
    final StringBuffer buf = new StringBuffer();

    buf.append(getClass().getName()).append('@').append(hashCode()).append(":");

    buf.append(" connected: ").append(isConnected());
    buf.append(", closed: ").append(isClosed());

    if (this.isSocketEndpoint.get()) {
      buf.append(" local=");
      if (this.localSocketAddress.isSet()) {
        buf.append(((TCSocketAddress) this.localSocketAddress.get()).getStringForm());
      } else {
        buf.append("[unknown]");
      }

      buf.append(" remote=");
      if (this.remoteSocketAddress.isSet()) {
        buf.append(((TCSocketAddress) this.remoteSocketAddress.get()).getStringForm());
      } else {
        buf.append("[unknown]");
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

    buf.append(" [").append(this.totalRead.get()).append(" read, ").append(this.totalWrite.get()).append(" write]");

    return buf.toString();
  }

  public final void addListener(final TCConnectionEventListener listener) {
    if (listener == null) { return; }
    this.eventListeners.add(listener); // don't need sync
  }

  public final void removeListener(final TCConnectionEventListener listener) {
    if (listener == null) { return; }
    this.eventListeners.remove(listener); // don't need sync
  }

  public final long getConnectTime() {
    return this.connectTime.get();
  }

  public final long getIdleTime() {
    return System.currentTimeMillis()
           - (this.lastDataWriteTime.get() > this.lastDataReceiveTime.get() ? this.lastDataWriteTime.get()
               : this.lastDataReceiveTime.get());
  }

  public final long getIdleReceiveTime() {
    return System.currentTimeMillis() - this.lastDataReceiveTime.get();
  }

  public final synchronized void connect(final TCSocketAddress addr, final int timeout) throws IOException,
      TCTimeoutException {
    if (this.closed.isSet() || this.connected.get()) { throw new IllegalStateException(
                                                                                       "Connection closed or already connected"); }
    connectImpl(addr, timeout);
    finishConnect();
  }

  public final synchronized boolean asynchConnect(final TCSocketAddress addr) throws IOException {
    if (this.closed.isSet() || this.connected.get()) { throw new IllegalStateException(
                                                                                       "Connection closed or already connected"); }

    final boolean rv = asynchConnectImpl(addr);

    if (rv) {
      finishConnect();
    }

    return rv;
  }

  public final void putMessage(final TCNetworkMessage message) {
    this.lastDataWriteTime.set(System.currentTimeMillis());

    // if (!isConnected() || isClosed()) {
    // logger.warn("Ignoring message sent to non-connected connection");
    // return;
    // }

    putMessageImpl(message);
  }

  public final TCSocketAddress getLocalAddress() {
    return (TCSocketAddress) this.localSocketAddress.get();
  }

  public final TCSocketAddress getRemoteAddress() {
    return (TCSocketAddress) this.remoteSocketAddress.get();
  }

  private final void setConnected(final boolean connected) {
    if (connected) {
      this.connectTime.set(System.currentTimeMillis());
    }
    this.connected.set(connected);
  }

  private final void recordSocketAddress(final Socket socket) throws IOException {
    if (socket != null) {
      final InetAddress localAddress = socket.getLocalAddress();
      final InetAddress remoteAddress = socket.getInetAddress();

      if (remoteAddress != null && localAddress != null) {
        this.isSocketEndpoint.set(true);
        this.localSocketAddress.set(new TCSocketAddress(cloneInetAddress(localAddress), socket.getLocalPort()));
        this.remoteSocketAddress.set(new TCSocketAddress(cloneInetAddress(remoteAddress), socket.getPort()));
      } else {
        // abort if socket is not connected
        throw new IOException("socket is not connected");
      }
    }
  }

  /**
   * This madness to workaround a SocketException("protocol family not available"). For whatever reason, the actual
   * InetAddress instances obtained directly from the connected socket has it's "family" field set to IPv6 even though
   * when it is an instance of Inet4Address. Trying to use that instance to connect to throws an exception
   */
  private static InetAddress cloneInetAddress(final InetAddress addr) {
    try {
      final byte[] address = addr.getAddress();
      return InetAddress.getByAddress(address);
    } catch (final UnknownHostException e) {
      throw new AssertionError(e);
    }
  }

  private final void addNetworkData(final TCByteBuffer[] data, final int length) {
    this.lastDataReceiveTime.set(System.currentTimeMillis());

    try {
      this.protocolAdaptor.addReadData(this, data, length);
    } catch (final Exception e) {
      logger.error(this.toString() + " " + e.getMessage());
      this.eventCaller.fireErrorEvent(this.eventListeners, this, e, null);
      return;
    }
  }

  protected final TCByteBuffer[] getReadBuffers() {
    // TODO: Hook in some form of read throttle. To throttle how much data is read from the network,
    // only return a subset of the buffers that the protocolAdaptor advises to be used.

    // TODO: should also support a way to de-register read interest temporarily

    return this.protocolAdaptor.getReadBuffers();
  }

  protected final void fireErrorEvent(final Exception e, final TCNetworkMessage context) {
    this.eventCaller.fireErrorEvent(this.eventListeners, this, e, context);
  }

  public final Socket detach() throws IOException {
    this.parent.removeConnection(this);
    return detachImpl();
  }

  private TCNetworkMessage buildWireProtocolMessageGroup(final ArrayList<TCNetworkMessage> messages) {
    Assert.assertTrue("Messages count not ok to build WireProtocolMessageGroup : " + messages.size(),
                      (messages.size() > 0) && (messages.size() <= WireProtocolHeader.MAX_MESSAGE_COUNT));
    if (messages.size() == 1) { return buildWireProtocolMessage(messages.get(0)); }

    final TCNetworkMessage message = WireProtocolGroupMessageImpl.wrapMessages(messages, this);
    Assert.eval(message.getSentCallback() == null);

    final Runnable[] callbacks = new Runnable[messages.size()];
    for (int i = 0; i < messages.size(); i++) {
      Assert.eval(!(messages.get(i) instanceof WireProtocolMessage));
      callbacks[i] = messages.get(i).getSentCallback();
    }

    message.setSentCallback(new Runnable() {
      public void run() {
        for (final Runnable callback : callbacks) {
          if (callback != null) {
            callback.run();
          }
        }
      }
    });
    return finalizeWireProtocolMessage((WireProtocolMessage) message, messages.size());
  }

  private TCNetworkMessage buildWireProtocolMessage(TCNetworkMessage message) {
    Assert.eval(!(message instanceof WireProtocolMessage));
    final TCNetworkMessage payload = message;

    message = WireProtocolMessageImpl.wrapMessage(message, this);
    Assert.eval(message.getSentCallback() == null);

    final Runnable callback = payload.getSentCallback();
    if (callback != null) {
      message.setSentCallback(new Runnable() {
        public void run() {
          callback.run();
        }
      });
    }
    return finalizeWireProtocolMessage((WireProtocolMessage) message, 1);
  }

  private TCNetworkMessage finalizeWireProtocolMessage(final WireProtocolMessage message, final int messageCount) {
    final WireProtocolHeader hdr = (WireProtocolHeader) message.getHeader();
    hdr.setSourceAddress(getLocalAddress().getAddressBytes());
    hdr.setSourcePort(getLocalAddress().getPort());
    hdr.setDestinationAddress(getRemoteAddress().getAddressBytes());
    hdr.setDestinationPort(getRemoteAddress().getPort());
    hdr.setMessageCount(messageCount);
    hdr.computeChecksum();
    return message;
  }

  private static class WriteContext {
    private final TCNetworkMessage message;
    private int                    index = 0;
    private final TCByteBuffer[]   entireMessageData;

    WriteContext(final TCNetworkMessage message) {
      // either WireProtocolMessage or WireProtocolMessageGroup
      this.message = message;

      if (MESSSAGE_PACKUP) {
        this.entireMessageData = getPackedUpMessage(message.getEntireMessageData());
      } else {
        this.entireMessageData = getClonedMessage(message.getEntireMessageData());
      }

    }

    boolean done() {
      for (int i = index, n = entireMessageData.length; i < n; i++) {
        if (entireMessageData[i].hasRemaining()) { return false; }
      }

      return true;
    }

    void incrementIndexAndCleanOld() {
      if (MESSSAGE_PACKUP) {
        // we created these new messages. lets recycle it.
        entireMessageData[index].recycle();
      }
      entireMessageData[index] = null;
      this.index++;
    }

    void writeComplete() {
      this.message.wasSent();
    }

    private static TCByteBuffer[] getClonedMessage(final TCByteBuffer[] sourceMessageByteBuffers) {
      final TCByteBuffer[] msgData = sourceMessageByteBuffers;
      TCByteBuffer[] clonedMessageData = new TCByteBuffer[msgData.length];
      for (int i = 0; i < msgData.length; i++) {
        clonedMessageData[i] = msgData[i].duplicate().asReadOnlyBuffer();
      }
      return clonedMessageData;
    }

    /**
     * Copies full message contents onto series of 4K chunk direct byte buffers. Since this routine operates on source
     * message byte buffer's backing arrays, these buffers shouldn't be readOnlyBuffers.
     */
    private static TCByteBuffer[] getPackedUpMessage(final TCByteBuffer[] sourceMessageByteBuffers) {

      int srcIndex = 0, srcOffset = 0, dstIndex = 0, srcRem = 0, dstRem = 0, written = 0, len = 0;
      for (TCByteBuffer sourceMessageByteBuffer : sourceMessageByteBuffers) {
        len += sourceMessageByteBuffer.remaining();
      }

      // packedup message is direct byte buffers based. so that system socket write can avoid copy over of data
      TCByteBuffer[] packedUpMessageByteBuffers = TCByteBufferFactory.getFixedSizedInstancesForLength(true, len);
      srcOffset = sourceMessageByteBuffers[srcIndex].arrayOffset();
      while (srcIndex < sourceMessageByteBuffers.length) {
        dstRem = packedUpMessageByteBuffers[dstIndex].remaining();
        srcRem = (sourceMessageByteBuffers[srcIndex].arrayOffset() + sourceMessageByteBuffers[srcIndex].limit())
                 - srcOffset;

        if (srcRem > dstRem) {
          packedUpMessageByteBuffers[dstIndex].put(sourceMessageByteBuffers[srcIndex].array(), srcOffset, dstRem);
          srcOffset += dstRem;
          dstIndex++;
          written += dstRem;
        } else if (srcRem == dstRem) {
          packedUpMessageByteBuffers[dstIndex].put(sourceMessageByteBuffers[srcIndex].array(), srcOffset, dstRem);
          dstIndex++;
          srcIndex++;
          srcOffset = ((srcIndex < sourceMessageByteBuffers.length) ? sourceMessageByteBuffers[srcIndex].arrayOffset()
              : 0);
          written += dstRem;
        } else {
          packedUpMessageByteBuffers[dstIndex].put(sourceMessageByteBuffers[srcIndex].array(), srcOffset, srcRem);
          srcIndex++;
          srcOffset = ((srcIndex < sourceMessageByteBuffers.length) ? sourceMessageByteBuffers[srcIndex].arrayOffset()
              : 0);
          written += srcRem;
        }
      }

      for (TCByteBuffer compactedMessageByteBuffer : packedUpMessageByteBuffers) {
        compactedMessageByteBuffer.flip();
      }

      if (len != written) {
        Assert.assertEquals("Comms Write: packed-up message length is different from original. ", len, written);
      }

      return packedUpMessageByteBuffers;
    }
  }

  public void addWeight(final int addWeightBy) {
    this.commWorker.addWeight(this, addWeightBy, this.channel);
  }

  public void setTransportEstablished() {
    this.transportEstablished.set(true);
  }

  public boolean isTransportEstablished() {
    return this.transportEstablished.get();
  }

}
