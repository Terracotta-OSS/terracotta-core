/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
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

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * The {@link TCConnection} implementation. SocketChannel read/write happens here.
 *
 * @author teck
 * @author mgovinda
 */
final class TCConnectionImpl implements TCConnection, TCChannelReader, TCChannelWriter {

  private static final long                     NO_CONNECT_TIME             = -1L;
  private static final Logger logger = LoggerFactory.getLogger(TCConnection.class);
  private static final long                     WARN_THRESHOLD              = 0x400000L;                                                    // 4MB

  private volatile CoreNIOServices              commWorker;
  private volatile SocketChannel                channel;
  private volatile BufferManager                bufferManager;
  private volatile PipeSocket                   pipeSocket;

  private final BufferManagerFactory            bufferManagerFactory;
  private final boolean                         clientConnection;              
  private final AtomicBoolean                   transportEstablished        = new AtomicBoolean(false);
  private final LinkedList<TCNetworkMessage>    writeMessages               = new LinkedList<TCNetworkMessage>();
  private final TCConnectionManagerImpl         parent;
  private final TCConnectionEventCaller         eventCaller                 = new TCConnectionEventCaller(logger);
  private final AtomicLong                      lastDataWriteTime           = new AtomicLong(System.currentTimeMillis());
  private final LongAdder                      messagesWritten           = new LongAdder();
  private final AtomicLong                      lastDataReceiveTime         = new AtomicLong(System.currentTimeMillis());
  private final LongAdder                      messagesRead           = new LongAdder();
  private final AtomicLong                      connectTime                 = new AtomicLong(NO_CONNECT_TIME);
  private final List<TCConnectionEventListener> eventListeners              = new CopyOnWriteArrayList<TCConnectionEventListener>();
  private final TCProtocolAdaptor               protocolAdaptor;
  private final AtomicBoolean                   isSocketEndpoint            = new AtomicBoolean(false);
  private final SetOnceFlag                     closed                      = new SetOnceFlag();
  private final AtomicBoolean                   connected                   = new AtomicBoolean(false);
  private final SetOnceRef<TCSocketAddress>     localSocketAddress          = new SetOnceRef<TCSocketAddress>();
  private final SetOnceRef<TCSocketAddress>     remoteSocketAddress         = new SetOnceRef<TCSocketAddress>();
  private final SocketParams                    socketParams;
  private final AtomicLong                      totalRead                   = new AtomicLong(0);
  private final AtomicLong                      totalWrite                  = new AtomicLong(0);
  private final ArrayList<WriteContext>         writeContexts               = new ArrayList<WriteContext>();
  private final Object                          pipeSocketWriteInterestLock = new Object();
  private boolean                               hasPipeSocketWriteInterest  = false;
  private int                                   writeBufferSize             = 0;

  private static final boolean                  MSG_GROUPING_ENABLED        = TCPropertiesImpl
                                                                                .getProperties()
                                                                                .getBoolean(TCPropertiesConsts.TC_MESSAGE_GROUPING_ENABLED);
  private static final int                      MSG_GROUPING_MAX_SIZE_BYTES = TCPropertiesImpl
                                                                                .getProperties()
                                                                                .getInt(TCPropertiesConsts.TC_MESSAGE_GROUPING_MAXSIZE_KB,
                                                                                        128) * 1024;
  private static final boolean                  MESSSAGE_PACKUP             = TCPropertiesImpl
                                                                                .getProperties()
                                                                                .getBoolean(TCPropertiesConsts.TC_MESSAGE_PACKUP_ENABLED,
                                                                                            true);
  private final Object                          readerLock                  = new Object();
  private final Object                          writerLock                  = new Object();

  static {
    logger.debug("Comms Message Batching " + (MSG_GROUPING_ENABLED ? "enabled" : "disabled"));
  }

  TCConnectionImpl(TCConnectionEventListener listener, TCProtocolAdaptor adaptor,
                   TCConnectionManagerImpl managerJDK14, CoreNIOServices nioServiceThread,
                   SocketParams socketParams, BufferManagerFactory bufferManagerFactory) {
    this(listener, adaptor, null, managerJDK14, nioServiceThread, socketParams, bufferManagerFactory);
  }

  TCConnectionImpl(TCConnectionEventListener listener, TCProtocolAdaptor adaptor, SocketChannel ch,
                   TCConnectionManagerImpl parent, CoreNIOServices nioServiceThread,
                   SocketParams socketParams, BufferManagerFactory bufferManagerFactory) {
    Assert.assertNotNull(parent);
    Assert.assertNotNull(adaptor);

    this.parent = parent;
    this.protocolAdaptor = adaptor;

    if (listener != null) {
      addListener(listener);
    }

    this.channel = ch;

    this.bufferManagerFactory = bufferManagerFactory;

    if (ch != null) {
      socketParams.applySocketParams(ch.socket());
      this.clientConnection = false;
    } else {
      this.clientConnection = true;
    }

    this.socketParams = socketParams;
    this.commWorker = nioServiceThread;
  }
  
  @Override
  public Map<String, ?> getState() {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("localAddress", this.getLocalAddress());
    state.put("remoteAddress", this.getRemoteAddress());
    state.put("totalRead", this.totalRead.get());
    state.put("totalWrite", this.totalWrite.get());
    state.put("connectTime", new Date(this.getConnectTime()));
    state.put("receiveIdleTime", this.getIdleReceiveTime());
    state.put("idleTime", this.getIdleTime());
    state.put("messageWritten", this.messagesWritten.longValue());
    state.put("messageRead", this.messagesRead.longValue());
    state.put("worker", commWorker.getName());
    state.put("closed", isClosed());
    state.put("connected", isConnected());
    state.put("closePending", isClosePending());
    state.put("transportConnected", isTransportEstablished());
    return state;
  }

  public void setCommWorker(CoreNIOServices worker) {
    this.commWorker = worker;
  }

  private void closeImpl(Runnable callback) {
    Assert.assertTrue(this.closed.isSet());
    this.transportEstablished.set(false);
    try {
      if (this.bufferManager != null) {
        this.bufferManager.close();
      }
    } catch (EOFException eof) {
      logger.debug("closed", eof);
    } catch (IOException ioe) {
      logger.warn("failed to close buffer manager", ioe);
    }
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
    try {
      if (pipeSocket != null) {
        synchronized (pipeSocketWriteInterestLock) {
          writeBufferSize = 0;
        }
        pipeSocket.dispose();
      }
    } catch (IOException ioe) {
      logger.warn("error closing pipesocket", ioe);
    }
  }

  protected void finishConnect() throws IOException {
    Assert.assertNotNull("channel", this.channel);
    Assert.assertNotNull("commWorker", this.commWorker);
    installBufferManager();
    recordSocketAddress(this.channel.socket());
    setConnected(true);
    this.eventCaller.fireConnectEvent(this.eventListeners, this);
  }

  private void connectImpl(TCSocketAddress addr, int timeout) throws IOException, TCTimeoutException {
    SocketChannel newSocket = null;
    final InetSocketAddress inetAddr = new InetSocketAddress(addr.getAddress(), addr.getPort());
    for (int i = 1; i <= 3; i++) {
      try {
        newSocket = createChannel();
        newSocket.configureBlocking(true);
        newSocket.socket().connect(inetAddr, timeout);
        newSocket.configureBlocking(false);
        break;
      } catch (final SocketTimeoutException ste) {
        Assert.eval(this.commWorker != null);
        this.commWorker.cleanupChannel(newSocket, null);
        throw new TCTimeoutException("Timeout of " + timeout + "ms occured connecting to " + addr, ste);
      }
    }
    this.channel = newSocket;
  }
  
  private void installBufferManager() throws IOException {
    this.bufferManager = bufferManagerFactory.createBufferManager(channel, clientConnection);
    if (this.bufferManager == null) {
      throw new IOException("buffer manager not provided");
    }
  }

  private SocketChannel createChannel() throws IOException, SocketException {
    final SocketChannel rv = SocketChannel.open();
    final Socket s = rv.socket();
    this.socketParams.applySocketParams(s);
    return rv;
  }

  private Socket detachImpl() throws IOException {
    this.pipeSocket = new PipeSocket(channel.socket()) {
      @Override
      public void onWrite() {
        synchronized (pipeSocketWriteInterestLock) {
          writeBufferSize++;
          if (!hasPipeSocketWriteInterest) {
            TCConnectionImpl.this.commWorker.requestWriteInterest(TCConnectionImpl.this, TCConnectionImpl.this.channel);
            hasPipeSocketWriteInterest = true;
          }
        }
      }

      @Override
      public synchronized void close() throws IOException {
        super.close();
        TCConnectionImpl.this.channel.socket().close();
      }
    };
    return pipeSocket;
  }

  private boolean asynchConnectImpl(TCSocketAddress address) throws IOException {
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

  @Override
  public int doRead() throws IOException {
    synchronized (readerLock) {
      return doReadInternal();
    }
  }

  private int doReadInternal() throws IOException {
    try {
      bufferManager.recvToBuffer();
    } catch (IOException ioe) {
      closeReadOnException(ioe);
      return 0;
    }

    int totalBytesReadFromBuffer = 0;
    int read;
    do {
      try {
        read = doReadFromBuffer();
        totalBytesReadFromBuffer += read;
      } catch (IOException ioe) {
        closeReadOnException(ioe);
        break;
      }
    } while (read != 0);

    this.totalRead.addAndGet(totalBytesReadFromBuffer);
    this.messagesRead.increment();
    return totalBytesReadFromBuffer;
  }

  public int doReadFromBuffer() throws IOException {
    if (pipeSocket != null) {
      return bufferManager.forwardFromReadBuffer(pipeSocket.getInputPipeSinkChannel());
    } else {
      return doReadFromBufferInternal();
    }
  }

  @Override
  public int doWrite() throws IOException {
    synchronized (writerLock) {
      return doWriteInternal();
    }
  }

  private int doWriteInternal() throws IOException {
    int written;
    try {
      written = doWriteToBuffer();
    } catch (IOException ioe) {
      closeWriteOnException(ioe);
      return 0;
    }

    int channelWritten = 0;
    while (channelWritten != written) {
      int sent;
      try {
        sent = bufferManager.sendFromBuffer();
      } catch (IOException ioe) {
        closeWriteOnException(ioe);
        break;
      }
      if (this.isClosePending() || this.isClosed()) {
        logger.debug("stop write due to closed connection");
        break;
      }
      channelWritten += sent;
    }
    this.totalWrite.addAndGet(channelWritten);
    return channelWritten;
  }

  private int doWriteToBuffer() throws IOException {
    if (pipeSocket != null) {
      synchronized (pipeSocketWriteInterestLock) {
        int gotFromSendBuffer = bufferManager.forwardToWriteBuffer(pipeSocket.getOutputPipeSourceChannel());
        writeBufferSize -= gotFromSendBuffer;

        if (writeBufferSize == 0 && hasPipeSocketWriteInterest) {
          TCConnectionImpl.this.commWorker.removeWriteInterest(TCConnectionImpl.this, TCConnectionImpl.this.channel);
          hasPipeSocketWriteInterest = false;
        }
        return gotFromSendBuffer;
      }
    } else {
      return doWriteToBufferInternal();
    }
  }

  private void buildWriteContextsFromMessages() {
    TCNetworkMessage messagesToWrite[];
    synchronized (this.writeMessages) {
      if (this.closed.isSet()) { return; }
      messagesToWrite = this.writeMessages.toArray(new TCNetworkMessage[this.writeMessages.size()]);
      this.writeMessages.clear();
    }
    ArrayList<TCNetworkMessage> currentBatch = (MSG_GROUPING_ENABLED
        ? new ArrayList<TCNetworkMessage>()
        : null);
    

    int batchSize = 0;
    int batchMsgCount = 0;
    for (final TCNetworkMessage element : messagesToWrite) {
      if (element instanceof WireProtocolMessage) {
        // we don't want to group already constructed Transport Handshake WireProtocolMessages
        final WireProtocolMessage ms = finalizeWireProtocolMessage((WireProtocolMessage) element, 1);
        this.writeContexts.add(new WriteContext(ms));
      } else if (WireProtocolHeader.PROTOCOL_UNKNOWN == WireProtocolHeader.getProtocolForMessageClass(element)) {
        // GenericNetwork messages are used for testing
        this.writeContexts.add(new WriteContext(element));
      } else if (MSG_GROUPING_ENABLED) {
        int realMessageSize = getRealMessgeSize(element.getTotalLength());
        if (!canBatch(realMessageSize, batchSize, batchMsgCount)) {
          // We can't add this to the current batch so seal the current batch as a write context and create a new one.
          this.writeContexts.add(new WriteContext(buildWireProtocolMessageGroup(currentBatch)));
          batchSize = 0;
          batchMsgCount = 0;
          currentBatch = new ArrayList<TCNetworkMessage>();
        }
        batchSize += realMessageSize;
        batchMsgCount++;
        currentBatch.add(element);
      } else {
        this.writeContexts.add(new WriteContext(buildWireProtocolMessage(element)));
      }
    }

    if (MSG_GROUPING_ENABLED && batchMsgCount > 0) {
      final WireProtocolMessage ms = buildWireProtocolMessageGroup(currentBatch);
      this.writeContexts.add(new WriteContext(ms));
    }
  }

  private boolean canBatch(int realMessageSize, int currentBatchSize, int currentBatchMsgCount) {
    // We can add this message to the batch if it fits, we don't already have too many messages in the batch
    //  OR if the message batch is currently empty (a degenerate case where a single message is too big to batch but
    //  we still want to send it).
    return (0 == currentBatchMsgCount) 
        || ((currentBatchSize + realMessageSize) <= MSG_GROUPING_MAX_SIZE_BYTES
          && (currentBatchMsgCount + 1 <= WireProtocolHeader.MAX_MESSAGE_COUNT));
  }

  private int getRealMessgeSize(int length) {
    return TCByteBufferFactory.getTotalBufferSizeNeededForMessageSize(length);
  }

  private int doReadFromBufferInternal() {
    final boolean debug = logger.isDebugEnabled();
    final TCByteBuffer[] readBuffers = getReadBuffers();

    int bytesRead = 0;
    // Do the read in a loop, instead of calling read(ByteBuffer[]).
    // This seems to avoid memory leaks on sun's 1.4.2 JDK
    for (final TCByteBuffer readBuffer : readBuffers) {
      final ByteBuffer buf = extractNioBuffer(readBuffer);

      if (buf.hasRemaining()) {
        final int read = bufferManager.forwardFromReadBuffer(buf);

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

    Assert.eval(bytesRead >= 0);

    if (debug) {
      logger.debug("Read " + bytesRead + " bytes on connection " + this.channel.toString());
    }

    addNetworkData(readBuffers, bytesRead);

    return bytesRead;
  }

  public int doWriteToBufferInternal() {
    final boolean debug = logger.isDebugEnabled();
    int totalBytesWritten = 0;

    // get a copy of the current write contexts. Since we call out to event/error handlers in the write
    // loop below, we don't want to be holding the lock on the writeContexts queue
    if (this.writeContexts.size() <= 0) {
      buildWriteContextsFromMessages();
    }
    while (this.writeContexts.size() > 0) {
      WriteContext context = this.writeContexts.get(0);
      final TCByteBuffer[] buffers = context.entireMessageData;

      long bytesWritten = 0;
      // Do the write in a loop, instead of calling write(ByteBuffer[]).
      // This seems to avoid memory leaks and faster
      for (int i = context.index, nn = buffers.length; i < nn; i++) {
        final int written = bufferManager.forwardToWriteBuffer(buffers[i].getNioBuffer());
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

  static private ByteBuffer extractNioBuffer(TCByteBuffer buffer) {
    return buffer.getNioBuffer();
  }

  private void putMessageImpl(TCNetworkMessage message) {
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

  @Override
  public final void asynchClose() {
    if (this.closed.attemptSet()) {
      closeImpl(createCloseCallback(null));
    } else {
      this.parent.removeConnection(this);
    }
  }

  @Override
  public final boolean close(long timeout) {
    if (timeout <= 0) { throw new IllegalArgumentException("timeout cannot be less than or equal to zero"); }

    if (this.closed.attemptSet()) {
      final CountDownLatch latch = new CountDownLatch(1);
      closeImpl(createCloseCallback(latch));
      try {
        return latch.await(timeout, TimeUnit.MILLISECONDS);
      } catch (final InterruptedException e) {
        logger.warn("close interrupted");
        Thread.currentThread().interrupt();
        return isConnected();
      }
    }

    return isClosed();
  }

  private Runnable createCloseCallback(final CountDownLatch latch) {
    final boolean fireClose = isConnected();

    return new Runnable() {
      @Override
      public void run() {
        setConnected(false);
        TCConnectionImpl.this.parent.connectionClosed(TCConnectionImpl.this);

        if (fireClose) {
          TCConnectionImpl.this.eventCaller.fireCloseEvent(TCConnectionImpl.this.eventListeners, TCConnectionImpl.this);
        }

        if (latch != null) {
          latch.countDown();
        }
      }
    };
  }

  @Override
  public final boolean isClosed() {
    return this.closed.isSet();
  }

  @Override
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
        buf.append(this.localSocketAddress.get().getStringForm());
      } else {
        buf.append("[unknown]");
      }

      buf.append(" remote=");
      if (this.remoteSocketAddress.isSet()) {
        buf.append(this.remoteSocketAddress.get().getStringForm());
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

  @Override
  public final void addListener(TCConnectionEventListener listener) {
    if (listener == null) { return; }
    this.eventListeners.add(listener); // don't need sync
  }

  @Override
  public final void removeListener(TCConnectionEventListener listener) {
    if (listener == null) { return; }
    this.eventListeners.remove(listener); // don't need sync
  }

  @Override
  public final long getConnectTime() {
    return this.connectTime.get();
  }

  @Override
  public final long getIdleTime() {
    return System.currentTimeMillis()
           - (this.lastDataWriteTime.get() > this.lastDataReceiveTime.get() ? this.lastDataWriteTime.get()
               : this.lastDataReceiveTime.get());
  }

  @Override
  public final long getIdleReceiveTime() {
    return System.currentTimeMillis() - this.lastDataReceiveTime.get();
  }

  @Override
  public final synchronized void connect(TCSocketAddress addr, int timeout) throws IOException,
      TCTimeoutException {
    if (this.closed.isSet() || this.connected.get()) { throw new IllegalStateException(
                                                                                       "Connection closed or already connected"); }
    connectImpl(addr, timeout);
    finishConnect();
    Assert.assertNotNull(this.commWorker);
    Assert.assertNotNull(this.bufferManager);
    this.commWorker.requestReadInterest(this, this.channel);
  }

  @Override
  public final synchronized boolean asynchConnect(TCSocketAddress addr) throws IOException {
    if (this.closed.isSet() || this.connected.get()) { throw new IllegalStateException(
                                                                                       "Connection closed or already connected"); }

    final boolean rv = asynchConnectImpl(addr);

    if (rv) {
      finishConnect();
    }

    return rv;
  }

  @Override
  public final void putMessage(TCNetworkMessage message) {
    this.lastDataWriteTime.set(System.currentTimeMillis());
    this.messagesWritten.increment();
    putMessageImpl(message);
  }

  @Override
  public final TCSocketAddress getLocalAddress() {
    if (this.localSocketAddress.isSet()) {
      return this.localSocketAddress.get();
    } else {
      return null;
    }
  }

  @Override
  public final TCSocketAddress getRemoteAddress() {
    if (this.remoteSocketAddress.isSet()) {
      return this.remoteSocketAddress.get();
    } else {
      return null;
    }
  }

  private void setConnected(boolean connected) {
    if (connected) {
      this.connectTime.set(System.currentTimeMillis());
    }
    this.connected.set(connected);
  }

  private void recordSocketAddress(Socket socket) throws IOException {
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
  private static InetAddress cloneInetAddress(InetAddress addr) {
    try {
      final byte[] address = addr.getAddress();
      return InetAddress.getByAddress(address);
    } catch (final UnknownHostException e) {
      throw new AssertionError(e);
    }
  }

  private final void addNetworkData(TCByteBuffer[] data, int length) {
    this.lastDataReceiveTime.set(System.currentTimeMillis());

    try {
      this.protocolAdaptor.addReadData(this, data, length);
    } catch (final Exception e) {
      logger.error(this.toString() + " " + e.getMessage());
      for (TCByteBuffer tcByteBuffer : data) {
        tcByteBuffer.clear();
      }
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

  protected final void fireErrorEvent(Exception e, TCNetworkMessage context) {
    this.eventCaller.fireErrorEvent(this.eventListeners, this, e, context);
  }

  @Override
  public final Socket detach() throws IOException {
    this.parent.removeConnection(this);
    return detachImpl();
  }

  private WireProtocolMessage buildWireProtocolMessageGroup(ArrayList<TCNetworkMessage> messages) {
    int messageGroupSize = messages.size();
    Assert.assertTrue("Messages count not ok to build WireProtocolMessageGroup : " + messageGroupSize,
        (messageGroupSize > 0) && (messageGroupSize <= WireProtocolHeader.MAX_MESSAGE_COUNT));
    if (messageGroupSize == 1) { return buildWireProtocolMessage(messages.get(0)); }

    final WireProtocolGroupMessageImpl message = WireProtocolGroupMessageImpl.wrapMessages(messages, this);
    Assert.eval(message.getSentCallback() == null);

    boolean hasNonNullCallbacks = false;
    final Runnable[] callbacks = new Runnable[messageGroupSize];
    for (int i = 0; i < messageGroupSize; i++) {
      TCNetworkMessage oneMessage = messages.get(i);
      Assert.eval(!(oneMessage instanceof WireProtocolMessage));
      Runnable callback = oneMessage.getSentCallback();
      if (null != callback) {
        callbacks[i] = callback;
        hasNonNullCallbacks = true;
      }
    }

    if (hasNonNullCallbacks) {
      message.setSentCallback(new Runnable() {
        @Override
        public void run() {
          for (final Runnable callback : callbacks) {
            if (callback != null) {
              callback.run();
            }
          }
        }
      });
    }
    return finalizeWireProtocolMessage(message, messageGroupSize);
  }

  private WireProtocolMessage buildWireProtocolMessage(TCNetworkMessage message) {
    Assert.eval(!(message instanceof WireProtocolMessage));
    final TCNetworkMessage payload = message;

    WireProtocolMessage wireMessage = WireProtocolMessageImpl.wrapMessage(message, this);
    Assert.eval(wireMessage.getSentCallback() == null);

    final Runnable callback = payload.getSentCallback();
    if (callback != null) {
      wireMessage.setSentCallback(callback);
    }
    return finalizeWireProtocolMessage(wireMessage, 1);
  }

  private WireProtocolMessage finalizeWireProtocolMessage(WireProtocolMessage message, int messageCount) {
    final WireProtocolHeader hdr = (WireProtocolHeader) message.getHeader();
    hdr.setSourceAddress(getLocalAddress().getAddressBytes());
    hdr.setSourcePort(getLocalAddress().getPort());
    hdr.setDestinationAddress(getRemoteAddress().getAddressBytes());
    hdr.setDestinationPort(getRemoteAddress().getPort());
    hdr.setMessageCount(messageCount);
    hdr.computeChecksum();
    return message;
  }

  public void closeReadOnException(IOException ioe) throws IOException {
    if (pipeSocket != null) {
      TCConnectionImpl.this.commWorker.removeReadInterest(TCConnectionImpl.this, TCConnectionImpl.this.channel);
      pipeSocket.closeRead();
    } else {
      if (ioe instanceof EOFException) {
        if (logger.isDebugEnabled()) {
          logger.debug("EOF reading from channel " + this.channel.toString());
        }
        this.eventCaller.fireEndOfFileEvent(this.eventListeners, this);
      } else {
        if (!isClosed()) {
          logger.info("error reading from channel " + this.channel.toString() + ": " + ioe.getMessage());
        } else if  (logger.isDebugEnabled()) {
          logger.debug("error reading from channel " + this.channel.toString() + ": " + ioe.getMessage());
        }

        this.eventCaller.fireErrorEvent(this.eventListeners, this, ioe, null);
      }
    }
  }

  @Override
  public boolean isClosePending() {
    return pipeSocket != null && pipeSocket.isClosed();
  }

  public void closeWriteOnException(IOException ioe) throws IOException {
    if (pipeSocket != null) {
      TCConnectionImpl.this.commWorker.removeWriteInterest(TCConnectionImpl.this, TCConnectionImpl.this.channel);
      pipeSocket.closeWrite();
    } else {
      if (ioe instanceof EOFException) {
        if (logger.isDebugEnabled()) {
          logger.debug("EOF writing to channel " + this.channel.toString());
        }
        this.eventCaller.fireEndOfFileEvent(this.eventListeners, this);
      } else {
        if (logger.isInfoEnabled()) {
          logger.info("error writing to channel " + this.channel.toString() + ": " + ioe.getMessage());
        }

        this.eventCaller.fireErrorEvent(this.eventListeners, this, ioe, null);
      }
    }
  }

  protected static class WriteContext {
    private final TCNetworkMessage message;
    private int                    index = 0;
    private final TCByteBuffer[]   entireMessageData;

    WriteContext(TCNetworkMessage message) {
      // either WireProtocolMessage or WireProtocolMessageGroup
      this.message = message;

      if (MESSSAGE_PACKUP && TCByteBufferFactory.isPoolingEnabled()) {
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

    private static TCByteBuffer[] getClonedMessage(TCByteBuffer[] sourceMessageByteBuffers) {
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
    protected static TCByteBuffer[] getPackedUpMessage(TCByteBuffer[] sourceMessageByteBuffers) {

      int srcIndex = 0, srcOffset = 0, dstIndex = 0, srcRem = 0, dstRem = 0, written = 0, len = 0;
      for (TCByteBuffer sourceMessageByteBuffer : sourceMessageByteBuffers) {
        len += sourceMessageByteBuffer.limit();
      }

      // packedup message is direct byte buffers based. so that system socket write can avoid copy over of data
      TCByteBuffer[] packedUpMessageByteBuffers = TCByteBufferFactory.getFixedSizedInstancesForLength(false, len);
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

  @Override
  public void addWeight(int addWeightBy) {
    this.commWorker.addWeight(this, addWeightBy, this.channel);
  }

  @Override
  public void setTransportEstablished() {
    this.transportEstablished.set(true);
  }

  @Override
  public boolean isTransportEstablished() {
    return this.transportEstablished.get();
  }

}
