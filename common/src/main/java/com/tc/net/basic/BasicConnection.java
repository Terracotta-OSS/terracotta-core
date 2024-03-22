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
package com.tc.net.basic;

import com.tc.bytes.TCReference;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.WireProtocolHeader;
import com.tc.net.protocol.transport.WireProtocolMessage;
import com.tc.net.protocol.transport.WireProtocolMessageImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.TCProtocolException;
import com.tc.net.protocol.tcm.TCActionNetworkMessage;
import com.tc.text.PrettyPrintable;
import java.io.Closeable;
import java.io.EOFException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tc.net.core.SocketEndpoint;
import com.tc.net.core.SocketEndpointFactory;
import com.tc.net.core.TCSocketEndpointReader;

/**
 *
 */
public class BasicConnection implements TCConnection {
  private static final Logger LOGGER = LoggerFactory.getLogger(BasicConnection.class);
  
  private long connect = 0;
  private volatile long last = System.currentTimeMillis();
  private volatile long received = System.currentTimeMillis();
  
  private final Consumer<TCConnection> closeRunnable;
  private final Consumer<WireProtocolMessage> write;
  private final TCProtocolAdaptor adaptor;
  private volatile SocketEndpoint socket;
  private final SocketEndpointFactory socketEndpointFactory;
  private volatile Socket src;
  private boolean established = false;
  private boolean connected = false;
  private final List<TCConnectionEventListener> listeners = new ArrayList<>();
  private volatile Thread serviceThread;
  private volatile ExecutorService readerExec;
  private final String id;
  
  
  public BasicConnection(String id, TCProtocolAdaptor adapter, SocketEndpointFactory buffers, Consumer<TCConnection> close) {
    this.id = id;
    this.socketEndpointFactory = buffers;
    Object writeMutex = new Object();
    this.write = (message)->{
      if (!message.prepareToSend()) {
        return;
      }
      synchronized (writeMutex) {
        try {
          if (this.src != null) {
            boolean interrupted = Thread.interrupted();
            try (TCReference data = message.getEntireMessageData().duplicate()) {
              long msgSize = data.available();
              ByteBuffer[] target = data.toByteBufferArray();
              try {
                while (data.hasRemaining()) {
                  switch(this.socket.writeFrom(target)) {
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
                    case SUCCESS:
                    case ZERO:
                      break;
                  }
                }
              } finally {
                data.returnByteBufferArray(target);
              }
            }
            if (interrupted) {
              Thread.currentThread().interrupt();
            }
          }
        } catch (IOException ioe) {
          fireError(ioe, message);
          close();
        } catch (Exception t) {
          fireError(t, message);
          close();
        } finally {
          message.complete();
        }
      }
    };
    this.closeRunnable = close;
    this.adaptor = adapter;
  }

  @Override
  public long getConnectTime() {
    return connect;
  }

  @Override
  public long getIdleTime() {
    return System.currentTimeMillis() - last;
  }

  @Override
  public long getIdleReceiveTime() {
    return System.currentTimeMillis() - received;
  }
  
  void markReceived() {
    received = System.currentTimeMillis();
  }

  @Override
  public synchronized void addListener(TCConnectionEventListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  @Override
  public synchronized void removeListener(TCConnectionEventListener listener) {
    if (listeners.contains(listener)) {
      listeners.remove(listener);
    }
  }

  @Override
  public void close() {
    try {
      asynchClose().get();
    } catch (ExecutionException e) {
      LOGGER.warn("close failed", e);
    } catch (InterruptedException e) {
      LOGGER.warn("close failed", e);
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public Future<Void> asynchClose() {
    try {
      this.closeRunnable.accept(this);
      if (src != null) {
        close(socket);
        SocketChannel channel = src.getChannel();
        tryOp(channel::shutdownInput);
        tryOp(channel::shutdownOutput);
        close(channel);
        close(src);
        LOGGER.debug("CLOSING {} channel {} isConnected: {} isConnectionPending: {}", System.identityHashCode(this), channel, channel.isConnected(), channel.isConnectionPending());
      }
      return shutdownAndAwaitTermination();
    } finally {
      this.established = false;
      this.connected = false;
      fireClosed();
    }
  }
  
  private void close(Closeable c) {
    try {
      c.close();
    } catch (IOException t) {
      LOGGER.debug("failed", t);
    }
  }
  
  private void tryOp(Callable op) {
    try {
      op.call();
    } catch (Exception t) {
      LOGGER.debug("failed", t);
    }
  }  
    
  private Future<Void> shutdownAndAwaitTermination() {
    ExecutorService reader = readerExec;
    if (reader != null) {
      reader.shutdownNow();
    }
    return new Future<Void>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return reader == null || reader.isTerminated();
      }

      @Override
      public Void get() throws InterruptedException, ExecutionException {
        if (reader != null) {
          reader.awaitTermination(0, TimeUnit.DAYS);
        }
        return null;
      }

      @Override
      public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (reader != null) {
          reader.awaitTermination(timeout, unit);
        }
        return null;
      } 
    };
  }

  private synchronized List<TCConnectionEventListener> getListeners() {
    return new ArrayList<>(listeners);
  }
  
  private void fireClosed() {
    TCConnectionEvent event = new TCConnectionEvent(this);
    getListeners().forEach(l->l.closeEvent(event));
  }
  
  private void fireConnect() {
    TCConnectionEvent event = new TCConnectionEvent(this);
    getListeners().forEach(l->l.connectEvent(event));
  }
  
  private void fireEOF() {
    TCConnectionEvent event = new TCConnectionEvent(this);
    getListeners().forEach(l->l.endOfFileEvent(event));
  }
  
  private void fireError(Exception err, TCNetworkMessage cxt) {
    TCConnectionErrorEvent event = new TCConnectionErrorEvent(this, err, cxt);
    getListeners().forEach(l->l.errorEvent(event));
  }
  
  @Override
  public synchronized Socket connect(InetSocketAddress addr, int timeout) throws IOException, TCTimeoutException {
    boolean interrupted = Thread.interrupted();
    Assert.assertNull(readerExec);
    Assert.assertNull(src);
    // always rebuild the socket address with exerything that comes with it UnkownHostException etc
    SocketChannel channel = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(addr.getHostString()), addr.getPort()));
    src = channel.socket();
    this.socket = socketEndpointFactory.createSocketEndpoint(channel, true);
    if (this.socket == null) {
      throw new IOException("buffer manager not provided");
    }
    this.connected = src.isConnected();
    if (connected) {
      readMessages();
      fireConnect();
      connect = System.currentTimeMillis();
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("connected", new Exception());
    }
    return src;
  }

  @Override
  public boolean asynchConnect(InetSocketAddress addr) throws IOException {
    try {
      connect(addr, 0);
    } catch (TCTimeoutException timeout) {
      throw new IOException(timeout);
    }
    return true;
  }

  @Override
  public synchronized boolean isConnected() {
    return this.connected;
  }

  @Override
  public synchronized boolean isClosed() {
    return !this.connected;
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return src != null ? new InetSocketAddress(this.src.getLocalAddress(), this.src.getLocalPort()) : null;
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return src != null ? new InetSocketAddress(this.src.getInetAddress(), this.src.getPort()) : null;
  }

  @Override
  public synchronized void setTransportEstablished() {
    established = true;
    LOGGER.debug("setting transport established");
  }

  @Override
  public synchronized boolean isTransportEstablished() {
    return established;
  }

  @Override
  public void putMessage(TCNetworkMessage message) {
    last = System.currentTimeMillis();
    WireProtocolMessage msg = buildWireProtocolMessage(message);
    if (msg != null) {
      this.write.accept(msg);
    }
  }
  
  private void readMessages() {
    Assert.assertNull(readerExec);
    readerExec = Executors.newFixedThreadPool(1, (r) -> {
      serviceThread = new Thread(r, id + " - BasicConnectionReader-" + this.src.getLocalSocketAddress() + "<-" + this.src.getRemoteSocketAddress() + " for (" + System.identityHashCode(this) + ")");
      serviceThread.setDaemon(true);
      return serviceThread;
    });
    LOGGER.debug("CREATED {} reader connected:{} established:{} reader:{}", System.identityHashCode(this), connected, established, readerExec);
    readerExec.submit(() -> {
      LOGGER.debug("STARTING {} reader connected:{} established:{}", System.identityHashCode(this), connected, established);
      boolean exiting = false;
      try (TCSocketEndpointReader reader = new TCSocketEndpointReader()) {
        while (!isClosed()) {
          LOGGER.debug("STATUS {} exiting:{} connected:{} established:{}", System.identityHashCode(this), exiting, connected, established);
          if (exiting) {
            return;
          }
          try (TCReference ref = reader.readFromSocket(socket, adaptor.getExpectedBytes())) {
            if (ref != null) {
              adaptor.addReadData(this, ref);
              markReceived();
            }
          } catch (EOFException eof) {
            if (!isClosed()) {
              fireEOF();
              close();
            }
            exiting = true;
          } catch (TCProtocolException | IOException ioe) {
            if (!isClosed()) {
              fireError(ioe, null);
              LOGGER.debug("error reading from connection", ioe);
              close();
            }
            exiting = true;
          }
          if (exiting) {
            LOGGER.debug("anticipate exiting connected:{} established:{}", connected, established);
          }
        }
      }
      LOGGER.debug("EXITED {} connected:{} established:{}", System.identityHashCode(this), connected, established);
    });
  }
    
  private WireProtocolMessage buildWireProtocolMessage(TCNetworkMessage message) {
    Objects.requireNonNull(message);
    if (message instanceof WireProtocolMessage) {
      return finalizeWireProtocolMessage((WireProtocolMessage)message);
    } else if (message instanceof TCActionNetworkMessage) {
      TCActionNetworkMessage action = (TCActionNetworkMessage)message;
      if (action.load() && action.commit()) {
        WireProtocolMessage wireMessage = WireProtocolMessageImpl.wrapMessage(action, this);
        return finalizeWireProtocolMessage(wireMessage);
      } 
    }
    
    return null;
  }

  private WireProtocolMessage finalizeWireProtocolMessage(WireProtocolMessage message) {
    final WireProtocolHeader hdr = (WireProtocolHeader) message.getHeader();
    hdr.setSourceAddress(getLocalAddress().getAddress().getAddress());
    hdr.setSourcePort(getLocalAddress().getPort());
    hdr.setDestinationAddress(getRemoteAddress().getAddress().getAddress());
    hdr.setDestinationPort(getRemoteAddress().getPort());
    hdr.setMessageCount(1);
    hdr.computeChecksum();
    return message;
  } 

  @Override
  public Map<String, ?> getState() {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("localAddress", this.getLocalAddress());
    state.put("remoteAddress", this.getRemoteAddress());
    state.put("connectTime", new Date(this.getConnectTime()));
    state.put("receiveIdleTime", this.getIdleReceiveTime());
    state.put("idleTime", this.getIdleTime());
    state.put("closed", isClosed());
    state.put("connected", isConnected());
    state.put("transportConnected", isTransportEstablished());
    if (socket instanceof PrettyPrintable) {
      state.put("buffer", ((PrettyPrintable)this.socket).getStateMap());
    } else {
      state.put("buffer", this.socket.toString());
    }
    return state;
  }  
}
