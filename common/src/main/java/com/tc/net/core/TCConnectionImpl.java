/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.core;

import com.tc.bytes.TCByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.bytes.TCDirectByteBufferCache;
import com.tc.bytes.TCReference;
import com.tc.bytes.TCReferenceSupport;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCDirectByteBufferOutputStream;
import com.tc.net.core.event.TCConnectionEventCaller;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.WireProtocolGroupMessageImpl;
import com.tc.net.protocol.transport.WireProtocolHeader;
import com.tc.net.protocol.transport.WireProtocolMessage;
import com.tc.net.protocol.transport.WireProtocolMessageImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import com.tc.net.protocol.TCProtocolAdaptor;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.tc.net.protocol.tcm.TCActionNetworkMessage;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The {@link TCConnection} implementation. SocketChannel read/write happens here.
 *
 * @author teck
 * @author mgovinda
 */
final class TCConnectionImpl implements TCConnection, TCChannelReader, TCChannelWriter {

  private static final long NO_CONNECT_TIME = -1L;
  private static final Logger logger = LoggerFactory.getLogger(TCConnection.class);
  private static final long WARN_THRESHOLD = 0x800000L;                                                    // 4MB

  private volatile CoreNIOServices commWorker;
  private volatile SocketChannel channel;
  private volatile SocketEndpoint socket;

  private final SocketEndpointFactory socketEndpointFactory;
  private final boolean clientConnection;
  private final AtomicBoolean transportEstablished = new AtomicBoolean(false);
  private final BlockingQueue<TCNetworkMessage> writeMessages = new ArrayBlockingQueue<>(MSG_GROUPING_MAX_COUNT);
  private final TCConnectionManagerImpl parent;
  private final TCDirectByteBufferCache buffers;
  private final TCSocketEndpointReader readAllocator;
  private final TCConnectionEventCaller eventCaller = new TCConnectionEventCaller(logger);
  private final AtomicLong lastDataWriteTime = new AtomicLong(System.currentTimeMillis());
  private final LongAdder messagesWritten = new LongAdder();
  private final LongAdder messageBatch = new LongAdder();
  private final AtomicLong lastDataReceiveTime = new AtomicLong(System.currentTimeMillis());
  private final LongAdder messagesRead = new LongAdder();
  private final AtomicLong connectTime = new AtomicLong(NO_CONNECT_TIME);
  private final Set<TCConnectionEventListener> eventListeners = ConcurrentHashMap.newKeySet();
  private final TCProtocolAdaptor protocolAdaptor;
  private final AtomicBoolean isSocketEndpoint = new AtomicBoolean(false);
  private final SetOnceFlag closed = new SetOnceFlag();
  private final AtomicBoolean connected = new AtomicBoolean(false);
  private final SetOnceRef<InetSocketAddress> localSocketAddress = new SetOnceRef<>();
  private final SetOnceRef<InetSocketAddress> remoteSocketAddress = new SetOnceRef<>();
  private final SocketParams socketParams;
  private final LongAdder totalRead = new LongAdder();
  private final LongAdder totalWrite = new LongAdder();
  private final Queue<WriteContext>  writeContexts = new ConcurrentLinkedQueue<>();
  private final ReentrantLock writeContextControl = new ReentrantLock();

  private static final boolean MSG_GROUPING_ENABLED = TCPropertiesImpl
                          .getProperties()
                          .getBoolean(TCPropertiesConsts.TC_MESSAGE_GROUPING_ENABLED);
  private static final int MSG_GROUPING_MAX_SIZE_BYTES = TCPropertiesImpl
                          .getProperties()
                          .getInt(TCPropertiesConsts.TC_MESSAGE_GROUPING_MAXSIZE_KB,
                                  128) * 1024;
  private static final int MSG_GROUPING_MAX_COUNT = TCPropertiesImpl
                          .getProperties()
                          .getInt(TCPropertiesConsts.TC_MESSAGE_GROUPING_MAX_COUNT,
                                  1024);
  private static final boolean MESSAGE_PACKUP = TCPropertiesImpl
                          .getProperties()
                          .getBoolean(TCPropertiesConsts.TC_MESSAGE_PACKUP_ENABLED,
                                      false);
  private final Object readerLock = new Object();
  private final Object writerLock = new Object();

  static {
    logger.debug("Comms Message Batching " + (MSG_GROUPING_ENABLED ? "enabled" : "disabled"));
  }

  TCConnectionImpl(TCConnectionEventListener listener, TCProtocolAdaptor adaptor,
                   TCConnectionManagerImpl managerJDK14, CoreNIOServices nioServiceThread,
                   SocketParams socketParams, SocketEndpointFactory bufferManagerFactory) {
    this(listener, adaptor, null, managerJDK14, nioServiceThread, socketParams, bufferManagerFactory);
  }

  TCConnectionImpl(TCConnectionEventListener listener, TCProtocolAdaptor adaptor, SocketChannel ch,
                   TCConnectionManagerImpl parent, CoreNIOServices nioServiceThread,
                   SocketParams socketParams, SocketEndpointFactory socketEndpointFactory) {
    Assert.assertNotNull(parent);
    Assert.assertNotNull(adaptor);

    this.parent = parent;
    this.protocolAdaptor = adaptor;

    if (listener != null) {
      addListener(listener);
    }

    this.channel = ch;

    this.socketEndpointFactory = socketEndpointFactory;

    if (ch != null) {
      socketParams.applySocketParams(ch.socket());
      this.clientConnection = false;
    } else {
      this.clientConnection = true;
    }

    this.socketParams = socketParams;
    this.commWorker = nioServiceThread;

    this.buffers = MESSAGE_PACKUP ? new TCDirectByteBufferCache(parent.getBufferCache()) : null;
    this.readAllocator = MESSAGE_PACKUP ? new TCSocketEndpointReader(buffers) : new TCSocketEndpointReader();
  }

  @Override
  public Map<String, ?> getState() {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("localAddress", this.getLocalAddress());
    state.put("remoteAddress", this.getRemoteAddress());
    state.put("totalRead", this.totalRead.sum());
    state.put("totalWrite", this.totalWrite.sum());
    state.put("connectTime", new Date(this.getConnectTime()));
    state.put("receiveIdleTime", this.getIdleReceiveTime());
    state.put("idleTime", this.getIdleTime());
    state.put("messageWritten", this.messagesWritten.longValue());
    state.put("messageBatch", this.messageBatch.longValue());
    state.put("messageRead", this.messagesRead.longValue());
    state.put("worker", commWorker.getName());
    state.put("closed", isClosed());
    state.put("connected", isConnected());
    state.put("transportConnected", isTransportEstablished());
    if (buffers != null) {
        state.put("buffers.type", "direct");
        state.put("buffers.cached", buffers.size());
        state.put("buffers.referenced", buffers.referenced());
    } else {
        state.put("buffers.type", "heap");
    }
    if (socket instanceof PrettyPrintable) {
      state.put("buffer", ((PrettyPrintable)this.socket).getStateMap());
    } else {
      state.put("buffer", this.socket.toString());
    }
    return state;
  }

  public boolean setCommWorker(CoreNIOServices worker) {
    if (this.commWorker != worker) {
      this.commWorker = worker;
      return true;
    } else {
      return false;
    }
  }

  private Future<Void> closeImpl(Runnable callback) {
    Assert.assertTrue(this.closed.isSet());
    this.transportEstablished.set(false);
    try {
      if (this.socket != null) {
        this.socket.close();
      }
    } catch (EOFException eof) {
      logger.debug("closed", eof);
    } catch (IOException ioe) {
      logger.warn("failed to close buffer manager", ioe);
    }

    if (this.channel != null) {
      CompletableFuture<Void> complete = new CompletableFuture<>();
      this.commWorker.cleanupChannel(this.channel, ()->{
        try {
          callback.run();
          cleanupUnsentWriteMessages();
          readAllocator.close();
          complete.complete(null);
        } catch (Exception e) {
          complete.completeExceptionally(e);
        }
      });
      return complete;
    } else {
      callback.run();
      cleanupUnsentWriteMessages();
      return CompletableFuture.completedFuture(null);
    }
  }

  private void cleanupUnsentWriteMessages() {
      this.writeMessages.forEach(TCNetworkMessage::complete);
      this.writeMessages.clear();
      this.writeContexts.forEach(WriteContext::writeComplete);
      this.writeContexts.clear();
  }

  protected void finishConnect() throws IOException {
    Assert.assertNotNull("channel", this.channel);
    Assert.assertNotNull("commWorker", this.commWorker);
    installBufferManager();
    recordSocketAddress(this.channel.socket());
    setConnected(true);
    this.eventCaller.fireConnectEvent(this.eventListeners, this);
  }

  private void connectImpl(InetSocketAddress addr, int timeout) throws IOException, TCTimeoutException {
    SocketChannel newSocket = null;
    // always rebuild the socket address with exerything that comes with it UnkownHostException etc
    final InetSocketAddress inetAddr = new InetSocketAddress(InetAddress.getByName(addr.getHostString()), addr.getPort());
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
    this.socket = socketEndpointFactory.createSocketEndpoint(channel, clientConnection);
    if (this.socket == null) {
      throw new IOException("buffer manager not provided");
    }
  }

  private SocketChannel createChannel() throws IOException, SocketException {
    final SocketChannel rv = SocketChannel.open();
    final Socket s = rv.socket();
    this.socketParams.applySocketParams(s);
    return rv;
  }

  private boolean asynchConnectImpl(InetSocketAddress address) throws IOException {
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
  public long doRead() throws IOException {
    synchronized (readerLock) {
      return doReadInternal();
    }
  }

  private long doReadInternal() throws IOException {
    try {
      long read = doReadAndPackage();
      this.totalRead.add(read);
      this.messagesRead.increment();
      return read;
    } catch (IOException ioe) {
      closeReadOnException(ioe);
    }
    return 0L;
  }

  @Override
  public long doWrite() throws IOException {
    synchronized (writerLock) {
      return doWriteInternal();
    }
  }

  private long doWriteInternal() throws IOException {
    long written;
    try {
      written = doPackageAndWrite();
    } catch (IOException ioe) {
      closeWriteOnException(ioe);
      return 0;
    }

    this.totalWrite.add(written);
    return written;
  }

  private boolean buildWriteContextsFromMessages(boolean failfast) {
    if (failfast) {
      if (!writeContextControl.tryLock()) {
        // take this opportunity to clear out any fully cancelled messages
        this.writeContexts.removeIf(WriteContext::isNotValid);
        return false;
      }
    } else {
      writeContextControl.lock();
    }

    try {
      if (!this.writeMessages.isEmpty()) {
        ArrayList<TCActionNetworkMessage> currentBatch = new ArrayList<>();
        int batchSize = 0;
        int batchMsgCount = 0;
        TCNetworkMessage element = this.writeMessages.poll();

        while (element != null) {
          if (this.closed.isSet()) {
            // dropping these messages.  make sure memory is returned
            currentBatch.forEach(TCActionNetworkMessage::complete);
            element.complete();
            return false;
          } else if (element instanceof WireProtocolMessage) {
              // we don't want to group already constructed Transport Handshake WireProtocolMessages
              final WireProtocolMessage ms = finalizeWireProtocolMessage((WireProtocolMessage) element, 1);
              this.writeContexts.add(new WriteContext(ms, 1));
          } else { // anything else that is sent on this path is based on a TCAction and needs to be wrapped
            TCActionNetworkMessage batchable = (TCActionNetworkMessage)element;
            if (batchable.load()) {
              int bytesToWrite = batchable.getTotalLength();
              if (bytesToWrite >= TCConnectionImpl.WARN_THRESHOLD) {
                logger.warn("Warning: Attempting to send a message (" + batchable.getClass().getName() + ") of size "
                            + bytesToWrite + " bytes");
              }
              if (MSG_GROUPING_ENABLED) {
                if (!canBatch(bytesToWrite, batchSize, batchMsgCount)) {
                  logger.debug("batching {} messages with size {}", batchMsgCount, batchSize);
                  // We can't add this to the current batch so seal the current batch as a write context and create a new one.
                  this.writeContexts.add(new WriteContext(buildWireProtocolMessageGroup(currentBatch), batchSize));
                  batchSize = 0;
                  currentBatch = new ArrayList<>(batchMsgCount);
                  batchMsgCount = 0;
                }
                batchSize += bytesToWrite;
                batchMsgCount++;
                currentBatch.add(batchable);
              } else {
                this.writeContexts.add(new WriteContext(buildWireProtocolMessage(batchable), 1));
              }
            } else {
              batchable.complete();
            }
          }

          element = this.writeMessages.poll();
        }

        if (MSG_GROUPING_ENABLED && batchMsgCount > 0) {
          final WireProtocolMessage ms = buildWireProtocolMessageGroup(currentBatch);
          this.writeContexts.add(new WriteContext(ms, batchMsgCount));
        }
        return true;
      } else {
        return false;
      }
    } finally {
      writeContextControl.unlock();
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

  private long doReadAndPackage() throws IOException {
    long size = 0;
    try (TCReference ref = readAllocator.readFromSocket(socket, this.protocolAdaptor.getExpectedBytes())) {
      if (ref != null) {
        size += ref.available();
        addNetworkData(ref);
      }
    }
    logger.debug("{} packaged {} bytes for {}", Thread.currentThread(), size, this);
    return size;
  }

  private long doPackageAndWrite() throws IOException {
    final boolean debug = logger.isDebugEnabled();
    long totalBytesWritten = 0;

    WriteContext context = this.writeContexts.poll();

    if (context == null) {
      if (buildWriteContextsFromMessages(true)) {
        context = this.writeContexts.poll();
      }
    }

    while (context != null) {
      long bytesWritten = context.write();
      messageBatch.increment();
      if (debug) {
        logger.debug("Wrote " + bytesWritten + " bytes on connection " + this.channel.toString() + " with batch size " + context.getBatchSize());
      }
      totalBytesWritten += bytesWritten;

      Assert.assertTrue(context.done());
      if (debug) {
        logger.debug("Complete message sent on connection " + this.channel.toString());
      }
      context.writeComplete();
      context = writeContexts.poll();
    }

    if (!this.closed.isSet() && context == null && !buildWriteContextsFromMessages(false)) {
      this.commWorker.removeWriteInterest(this, this.channel);
    }

    return totalBytesWritten;
  }

  private void putMessageImpl(TCNetworkMessage message) {
    // ??? Does the message queue and the WriteContext belong in the base connection class?
    final boolean debug = logger.isDebugEnabled();

    boolean placed = false;
    boolean newData = false;

    while (!placed) {
      if (this.closed.isSet()) { message.complete(); return; }
      placed = this.writeMessages.offer(message);
      if (!placed) {
        buildWriteContextsFromMessages(true);
      } else {
        newData = this.writeMessages.peek() == message;
      }
    }

    if (debug) {
      logger.debug("Connection (" + this.channel.toString() + ") has " + this.writeMessages.size() + " messages queued");
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
  public void close() {
    try {
      asynchClose().get();
    } catch (ExecutionException exception) {
      logger.warn("error closing connection", exception);
    } catch(InterruptedException exception) {
      logger.warn("interrupted closing connection", exception);
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public Future<Void> asynchClose() {
    if (this.closed.attemptSet()) {
      return closeImpl(createCloseCallback());
    } else {
      this.parent.removeConnection(this);
      return CompletableFuture.completedFuture(null);
    }
  }

  private Runnable createCloseCallback() {
    final boolean fireClose = isConnected();

    return new Runnable() {
      @Override
      public void run() {
        logger.debug("running close callback");
        setConnected(false);
        TCConnectionImpl.this.parent.connectionClosed(TCConnectionImpl.this);

        if (fireClose) {
          TCConnectionImpl.this.eventCaller.fireCloseEvent(TCConnectionImpl.this.eventListeners, TCConnectionImpl.this);
        }

        if (buffers != null) {
          buffers.close();
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
        buf.append(this.localSocketAddress.get().toString());
      } else {
        buf.append("[unknown]");
      }

      buf.append(" remote=");
      if (this.remoteSocketAddress.isSet()) {
        buf.append(this.remoteSocketAddress.get().toString());
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

    buf.append(" [").append(this.totalRead.sum()).append(" read, ").append(this.totalWrite.sum()).append(" write]");

    buf.append(" buffer=").append(this.socket);

    return buf.toString();
  }

  @Override
  public final boolean addListener(TCConnectionEventListener listener) {
    if (listener == null) { return false; }
    return this.eventListeners.add(listener);
  }

  @Override
  public final boolean removeListener(TCConnectionEventListener listener) {
    if (listener == null) { return false; }
    return this.eventListeners.remove(listener);
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
  public final synchronized Socket connect(InetSocketAddress addr, int timeout) throws IOException,
      TCTimeoutException {
    if (this.closed.isSet() || this.connected.get()) { throw new IllegalStateException(
                                                                                       "Connection closed or already connected"); }
    connectImpl(addr, timeout);
    finishConnect();
    Assert.assertNotNull(this.commWorker);
    Assert.assertNotNull(this.socket);
    this.commWorker.requestReadInterest(this, this.channel);
    return this.channel.socket();
  }

  @Override
  public final synchronized boolean asynchConnect(InetSocketAddress addr) throws IOException {
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
  public final InetSocketAddress getLocalAddress() {
    if (this.localSocketAddress.isSet()) {
      return this.localSocketAddress.get();
    } else {
      return null;
    }
  }

  @Override
  public final InetSocketAddress getRemoteAddress() {
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
        this.localSocketAddress.set(new InetSocketAddress(cloneInetAddress(localAddress), socket.getLocalPort()));
        this.remoteSocketAddress.set(new InetSocketAddress(cloneInetAddress(remoteAddress), socket.getPort()));
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

  private void addNetworkData(TCReference data) {
    this.lastDataReceiveTime.set(System.currentTimeMillis());

    try {
      this.protocolAdaptor.addReadData(this, data);
    } catch (final Exception e) {
      logger.error(this.toString() + " " + e.getMessage());
      for (TCByteBuffer tcByteBuffer : data) {
        tcByteBuffer.clear();
      }
      this.eventCaller.fireErrorEvent(this.eventListeners, this, e, null);
    }
  }

  protected final void fireErrorEvent(Exception e, TCNetworkMessage context) {
    this.eventCaller.fireErrorEvent(this.eventListeners, this, e, context);
  }

  private WireProtocolMessage buildWireProtocolMessageGroup(ArrayList<TCActionNetworkMessage> messages) {
    int messageGroupSize = messages.size();
    Assert.assertTrue("Messages count not ok to build WireProtocolMessageGroup : " + messageGroupSize,
        (messageGroupSize > 0) && (messageGroupSize <= WireProtocolHeader.MAX_MESSAGE_COUNT));
    if (messageGroupSize == 1) { return buildWireProtocolMessage(messages.get(0)); }

    final WireProtocolGroupMessageImpl message = WireProtocolGroupMessageImpl.wrapMessages(messages, this);

    return finalizeWireProtocolMessage(message, messageGroupSize);
  }

  private WireProtocolMessage buildWireProtocolMessage(TCActionNetworkMessage message) {
    Assert.eval(!(message instanceof WireProtocolMessage));

    WireProtocolMessage wireMessage = WireProtocolMessageImpl.wrapMessage(message, this);

    return finalizeWireProtocolMessage(wireMessage, 1);
  }

  private WireProtocolMessage finalizeWireProtocolMessage(WireProtocolMessage message, int messageCount) {
    final WireProtocolHeader hdr = (WireProtocolHeader) message.getHeader();
    hdr.setSourceAddress(getLocalAddress().getAddress().getAddress());
    hdr.setSourcePort(getLocalAddress().getPort());
    hdr.setDestinationAddress(getLocalAddress().getAddress().getAddress());
    hdr.setDestinationPort(getRemoteAddress().getPort());
    hdr.setMessageCount(messageCount);

    if (logger.isDebugEnabled()) {
      logger.debug("finalize header " + hdr);
    }
    return message;
  }

  public void closeReadOnException(IOException ioe) throws IOException {
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

  public void closeWriteOnException(IOException ioe) throws IOException {
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

  @Override
  public TCByteBufferOutputStream createOutput() {
    return MESSAGE_PACKUP ? new TCDirectByteBufferOutputStream(buffers) : new TCByteBufferOutputStream();
  }

  protected class WriteContext {
    private final WireProtocolMessage message;
    private final int batchSize;
    private boolean sent = false;

    WriteContext(WireProtocolMessage message, int batchSize) {
      this.message = message;
      this.batchSize = batchSize;
    }

    private TCReference prep() {
      if (message.prepareToSend()) {
        return message.getEntireMessageData().duplicate();
      } else {
        return TCReferenceSupport.createGCReference(Collections.emptyList());
      }
    }

    boolean done() {
      return sent;
    }

    void writeComplete() {
      this.message.complete();
    }

    boolean isNotValid() {
      return !message.isValid();
    }

    int getBatchSize() {
      return batchSize;
    }

    long write() throws IOException {
      long written = 0;
      try (TCReference msgRef = prep()) {
        long msgSize = msgRef.available();
        ByteBuffer[] compat = msgRef.toByteBufferArray();
        try {
          while (msgRef.hasRemaining()) {
            switch (socket.writeFrom(compat)) {
              case SUCCESS:
              case ZERO:
                break;
              case EOF:
                throw new EOFException();
              case OVERFLOW:
                // unexpected
                throw new IOException();
              case UNDERFLOW:
                if (msgSize > 0) {
                  throw new IOException("underflow");
                }
                break;
                // not sure what to do here.  need to figure out if there is some way to
                // send dummy bytes
            }
          }
          sent = true;
          written = msgSize - msgRef.available();
        } finally {
          msgRef.returnByteBufferArray(compat);
        }
      }
      return written;
    }
  }

  @Override
  public void setTransportEstablished() {
    this.commWorker.addConnection(this, this.channel);
    this.transportEstablished.set(true);
  }

  public void migrate() {
    if (this.commWorker.getReaderComm() == Thread.currentThread()) {
      this.commWorker.addConnection(this, this.channel);
    } else {
      this.commWorker.getReaderComm().addSelectorTask(()-> {
        this.commWorker.addConnection(this, this.channel);
      });
    }
  }

  @Override
  public boolean isTransportEstablished() {
    return this.transportEstablished.get();
  }

}
