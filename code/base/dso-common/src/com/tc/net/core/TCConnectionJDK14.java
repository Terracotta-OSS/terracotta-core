/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.NIOWorkarounds;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.SetOnceFlag;
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
import java.util.LinkedList;

/**
 * JDK14 (nio) implementation of TCConnection
 *
 * @author teck
 */
final class TCConnectionJDK14 extends AbstractTCConnection implements TCJDK14ChannelReader, TCJDK14ChannelWriter {
  private static final long      WARN_THRESHOLD = 0x400000L;        // 4MB
  private final SetOnceFlag      closed         = new SetOnceFlag();
  private final LinkedList       writeContexts  = new LinkedList();
  private final TCCommJDK14      comm;
  private volatile SocketChannel channel;

  // for creating unconnected client connections
  TCConnectionJDK14(TCConnectionEventListener listener, TCCommJDK14 comm, TCProtocolAdaptor adaptor,
                    AbstractTCConnectionManager parent) {
    this(listener, comm, adaptor, null, parent);
  }

  TCConnectionJDK14(TCConnectionEventListener listener, TCCommJDK14 comm, TCProtocolAdaptor adaptor, SocketChannel ch,
                    AbstractTCConnectionManager parent) {
    super(listener, adaptor, parent);

    Assert.assertNotNull(comm);
    this.comm = comm;
    this.channel = ch;
  }

  protected void closeImpl(Runnable callback) {
    try {
      if (channel != null) {
        comm.cleanupChannel(channel, callback);
      } else {
        callback.run();
      }
    } finally {
      synchronized (writeContexts) {
        // this will blow up if it is set more than once. Super class should not allow this closeImpl() to
        // be called more than once
        closed.set();

        writeContexts.clear();
      }
    }
  }

  protected void finishConnect() {
    Assert.assertNotNull("channel", channel);
    recordSocketAddress(channel.socket());
    super.finishConnect();
  }

  protected void connectImpl(TCSocketAddress addr, int timeout) throws IOException, TCTimeoutException {
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

  protected Socket detachImpl() throws IOException {
    comm.unregister(channel);
    channel.configureBlocking(true);
    return channel.socket();
  }

  protected boolean asynchConnectImpl(TCSocketAddress address) throws IOException {
    SocketChannel newSocket = createChannel();
    newSocket.configureBlocking(false);

    InetSocketAddress inetAddr = new InetSocketAddress(address.getAddress(), address.getPort());
    final boolean connected = newSocket.connect(inetAddr);
    setConnected(connected);

    channel = newSocket;

    if (!connected) {
      comm.requestConnectInterest(this, newSocket);
    }

    return connected;
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

      fireErrorEvent(ioe, null);
      return;
    }

    if (readEOF) {
      if (bytesRead > 0) {
        addNetworkData(readBuffers, bytesRead);
      }

      if (debug) logger.debug("EOF read on connection " + channel.toString());

      fireEndOfFileEvent();
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

        fireErrorEvent(ioe, context.message);
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
      rv[i] = (ByteBuffer) src[i].getNioBuffer();
    }

    return rv;
  }

  static private ByteBuffer extractNioBuffer(TCByteBuffer buffer) {
    return (ByteBuffer) buffer.getNioBuffer();
  }

  protected void putMessageImpl(TCNetworkMessage message) {
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
