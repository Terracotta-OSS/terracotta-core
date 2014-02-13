/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.CustomerLogging;
import com.tc.logging.LossyTCLogger;
import com.tc.logging.LossyTCLogger.LossyTCLoggerType;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.ReconnectionRejectedException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressIterator;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.Util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This guy establishes a connection to the server for the Client.
 */
public class ClientConnectionEstablisher {

  private static final TCLogger             LOGGER                = TCLogging
                                                                      .getLogger(ClientConnectionEstablisher.class);

  private static final long                 CONNECT_RETRY_INTERVAL;
  private static final long                 MIN_RETRY_INTERVAL    = 10;
  public static final String                RECONNECT_THREAD_NAME = "ConnectionEstablisher";

  private final String                      desc;
  private final int                         maxReconnectTries;
  private final int                         timeout;
  private final ConnectionAddressProvider   connAddressProvider;
  private final TCConnectionManager         connManager;
  private final AtomicBoolean               asyncReconnecting     = new AtomicBoolean(false);
  private final AtomicBoolean               allowReconnects       = new AtomicBoolean(true);
  private volatile AsyncReconnect           asyncReconnect;

  private final ReconnectionRejectedHandler reconnectionRejectedHandler;

  static {
    TCLogger logger = TCLogging.getLogger(ClientConnectionEstablisher.class);
    long value = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.L1_SOCKET_RECONNECT_WAIT_INTERVAL);
    if (value < MIN_RETRY_INTERVAL) {
      logger.warn("Forcing reconnect wait interval to " + MIN_RETRY_INTERVAL + " (configured value was " + value + ")");
      value = MIN_RETRY_INTERVAL;
    }

    CONNECT_RETRY_INTERVAL = value;
  }

  public ClientConnectionEstablisher(TCConnectionManager connManager, ConnectionAddressProvider connAddressProvider,
                                     int maxReconnectTries, int timeout,
                                     ReconnectionRejectedHandler reconnectionRejectedHandler) {
    this.connManager = connManager;
    this.connAddressProvider = connAddressProvider;
    this.maxReconnectTries = maxReconnectTries;
    this.timeout = timeout;
    this.reconnectionRejectedHandler = reconnectionRejectedHandler;
    this.asyncReconnect = new AsyncReconnect(this);

    if (maxReconnectTries == 0) {
      this.desc = "none";
    } else if (maxReconnectTries < 0) {
      this.desc = "unlimited";
    } else {
      this.desc = "" + maxReconnectTries;
    }

  }

  public void reset() {
    quitReconnectAttempts();
    this.asyncReconnect = new AsyncReconnect(this);
  }

  // method fore testing only
  AsyncReconnect getAsyncReconnectThread() {
    return asyncReconnect;
  }

  // method used for testing only
  void setAsyncReconnectingForTests(boolean val) {
    this.asyncReconnecting.set(val);
  }

  // for testing only
  void setAsyncReconnectThreadForTests(AsyncReconnect asyncReconnThread) {
    this.asyncReconnect = asyncReconnThread;
  }

  // method used in testing only
  void setAllowReconnects(boolean val) {
    this.allowReconnects.set(val);
  }

  // method used in testing only
  boolean getAllowReconnects() {
    return this.allowReconnects.get();
  }

  /**
   * Blocking open. Causes a connection to be made. Will throw exceptions if the connect fails.
   * 
   * @throws TCTimeoutException
   * @throws IOException
   * @throws CommStackMismatchException
   * @throws MaxConnectionsExceededException
   */
  public void open(ClientMessageTransport cmt) throws TCTimeoutException, IOException, MaxConnectionsExceededException,
      CommStackMismatchException {
    synchronized (this.asyncReconnecting) {
      Assert.eval("Can't call open() while asynch reconnect occurring", !this.asyncReconnecting.get());
      this.allowReconnects.set(true);
      connectTryAllOnce(cmt);
    }
  }

  void connectTryAllOnce(ClientMessageTransport cmt) throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    final ConnectionAddressIterator addresses = this.connAddressProvider.getIterator();
    TCConnection rv = null;
    while (addresses.hasNext()) {
      final ConnectionInfo connInfo = addresses.next();
      try {
        final TCSocketAddress csa = new TCSocketAddress(connInfo);
        rv = connect(csa, cmt);
        cmt.openConnection(rv);
        break;
      } catch (TCTimeoutException e) {
        if (!addresses.hasNext()) { throw e; }
      } catch (IOException e) {
        if (!addresses.hasNext()) { throw e; }
      }
    }
  }

  /**
   * Tries to make a connection. This is a blocking call.
   * 
   * @return
   * @throws TCTimeoutException
   * @throws IOException
   * @throws MaxConnectionsExceededException
   */
  TCConnection connect(TCSocketAddress sa, ClientMessageTransport cmt) throws TCTimeoutException, IOException {

    TCConnection connection = this.connManager.createConnection(cmt.getProtocolAdapter());
    cmt.fireTransportConnectAttemptEvent();
    try {
      connection.connect(sa, this.timeout);
    } catch (IOException e) {
      connection.close(100);
      throw e;
    } catch (TCTimeoutException e) {
      throw e;
    }
    return connection;
  }

  @Override
  public String toString() {
    return "ClientConnectionEstablisher[" + this.connAddressProvider + ", timeout=" + this.timeout + "]";
  }

  void reconnect(ClientMessageTransport cmt) throws MaxConnectionsExceededException {
    try {
      // Lossy logging for connection errors. Log the errors once in every 10 seconds
      LossyTCLogger connectionErrorLossyLogger = new LossyTCLogger(cmt.getLogger(), 10000,
                                                                   LossyTCLoggerType.TIME_BASED, true);

      boolean connected = cmt.isConnected();
      if (connected) {
        cmt.getLogger().warn("Got reconnect request for ClientMessageTransport that is connected.  skipping");
        return;
      }

      this.asyncReconnecting.set(true);
      boolean reconnectionRejected = false;
      for (int i = 0; ((this.maxReconnectTries < 0) || (i < this.maxReconnectTries)) && isReconnectBetweenL2s()
                      && !connected; i++) {
        ConnectionAddressIterator addresses = this.connAddressProvider.getIterator();
        while (addresses.hasNext() && !connected && isReconnectBetweenL2s()) {

          if (reconnectionRejected) {
            if (reconnectionRejectedHandler.isRetryOnReconnectionRejected()) {
              LOGGER.info("Reconnection rejected by L2, trying again to reconnect - " + cmt);
            } else {
              LOGGER.info("Reconnection rejected by L2, no more trying to reconnect - " + cmt);
              return;
            }
          }

          TCConnection connection = null;
          final ConnectionInfo connInfo = addresses.next();

          // DEV-1945
          if (i == 0) {
            String previousConnectHost = "";
            int previousConnectHostPort = -1;
            if (cmt.getRemoteAddress() != null) {
              previousConnectHost = cmt.getRemoteAddress().getAddress().getHostAddress();
              previousConnectHostPort = cmt.getRemoteAddress().getPort();
            }
            String connectingToHost = "";
            try {
              connectingToHost = InetAddress.getByName(connInfo.getHostname()).getHostAddress();
            } catch (UnknownHostException e) {
              // these errors are caught even before
              throw new RuntimeException(e);
            }
            int connectingToHostPort = connInfo.getPort();

            if ((addresses.hasNext()) && (previousConnectHost.equals(connectingToHost))
                && (previousConnectHostPort == connectingToHostPort)) {
              continue;
            }
          }
          try {
            if (i % 20 == 0) {
              cmt.getLogger().warn("Reconnect attempt " + i + " of " + this.desc + " reconnect tries to " + connInfo
                                       + ", timeout=" + this.timeout);
            }
            connection = connect(new TCSocketAddress(connInfo), cmt);
            cmt.reconnect(connection);
            connected = true;
          } catch (MaxConnectionsExceededException e) {
            throw e;
          } catch (ReconnectionRejectedException e) {
            reconnectionRejected = true;
            handleConnectException(e, false, connectionErrorLossyLogger, connection);
          } catch (TCTimeoutException e) {
            handleConnectException(e, true, connectionErrorLossyLogger, connection);
          } catch (IOException e) {
            handleConnectException(e, false, connectionErrorLossyLogger, connection);
          } catch (Exception e) {
            handleConnectException(e, true, connectionErrorLossyLogger, connection);
          }
        }
      }
    } finally {
      asyncReconnecting.set(false);
    }
  }

  // TRUE for L2, for L1 only if not stopped
  boolean isReconnectBetweenL2s() {
    return reconnectionRejectedHandler.isRetryOnReconnectionRejected() || !asyncReconnect.isStopped();
  }

  void restoreConnection(ClientMessageTransport cmt, TCSocketAddress sa, long timeoutMillis,
                         RestoreConnectionCallback callback) throws MaxConnectionsExceededException {
    final long deadline = System.currentTimeMillis() + timeoutMillis;
    boolean connected = cmt.isConnected();
    if (connected) {
      cmt.getLogger().warn("Got restoreConnection request for ClientMessageTransport that is connected.  skipping");
      return;
    }

    this.asyncReconnecting.set(true);
    try {
      boolean reconnectionRejected = false;
      while (!connected && isReconnectBetweenL2s()) {

        if (reconnectionRejected) {
          if (reconnectionRejectedHandler.isRetryOnReconnectionRejected()) {
            LOGGER.info("Reconnection rejected by L2, trying again to restore connection - " + cmt);
          } else {
            LOGGER.info("Reconnection rejected by L2, no more trying to restore connection - " + cmt);
            return;
          }
        }
        TCConnection connection = null;
        try {
          connection = connect(sa, cmt);
          cmt.reconnect(connection);
          connected = true;
        } catch (MaxConnectionsExceededException e) {
          callback.restoreConnectionFailed(cmt);
          // DEV-2781
          throw e;
        } catch (TCTimeoutException e) {
          handleConnectException(e, true, cmt.getLogger(), connection);
        } catch (ReconnectionRejectedException e) {
          reconnectionRejected = true;
          handleConnectException(e, false, cmt.getLogger(), connection);
        } catch (IOException e) {
          handleConnectException(e, false, cmt.getLogger(), connection);
        } catch (Exception e) {
          handleConnectException(e, true, cmt.getLogger(), connection);
        }
        if (connected || System.currentTimeMillis() > deadline) {
          break;
        }
      }
      if (!connected && !reconnectionRejected) {
        callback.restoreConnectionFailed(cmt);
      }
    } finally {
      asyncReconnecting.set(false);
    }
  }

  private void handleConnectException(Exception e, boolean logFullException, TCLogger logger, TCConnection connection) {
    if (connection != null) {
      connection.close(100);
    }

    if (logger.isDebugEnabled() || logFullException) {
      logger.error("Connect Exception", e);
    } else {
      logger.warn(e.getMessage());
    }

    if (CONNECT_RETRY_INTERVAL > 0) {
      try {
        Thread.sleep(CONNECT_RETRY_INTERVAL);
      } catch (InterruptedException e1) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public void asyncReconnect(ClientMessageTransport cmt) {
    putConnectionRequest(ConnectionRequest.newReconnectRequest(cmt));
  }

  public void asyncRestoreConnection(ClientMessageTransport cmt, TCSocketAddress sa,
                                     RestoreConnectionCallback callback, long timeoutMillis) {
    putConnectionRequest(ConnectionRequest.newRestoreConnectionRequest(cmt, sa, callback, timeoutMillis));
  }

  void switchAsyncReconnectForTests(AsyncReconnect tmpAsyncReconnect) {
    this.asyncReconnect = tmpAsyncReconnect;
  }

  private void putConnectionRequest(ConnectionRequest request) {
    if (!this.allowReconnects.get() || asyncReconnect.isStopped()) {
      LOGGER.info("Ignoring connection request: " + request + " as allowReconnects: " + allowReconnects.get()
                  + ", asyncReconnect.isStopped(): " + asyncReconnect.isStopped());
      return;
    }
    // Allow the async thread reconnects/restores only when cmt was connected atleast once
    if (request.getClientMessageTransport() != null && request.getClientMessageTransport().wasOpened()) {
      asyncReconnect.startThreadIfNecessary();
      this.asyncReconnect.putConnectionRequest(request);
    } else {
      LOGGER.info("Ignoring connection request as transport was not connected even once");
    }
  }

  public void quitReconnectAttempts() {
    allowReconnects.set(false);
    asyncReconnect.stop();
    if (!isReconnectBetweenL2s()) {
      this.asyncReconnect.awaitTermination(true);
    }
  }

  // for testing only
  int connectionRequestQueueSize() {
    return this.asyncReconnect.connectionRequestQueueSize();
  }

  static class AsyncReconnect implements Runnable {
    private static final TCLogger             logger             = TCLogging.getLogger(AsyncReconnect.class);
    private final ClientConnectionEstablisher cce;
    private final AtomicBoolean               threadStarted      = new AtomicBoolean(false);
    private volatile boolean                  stopped            = false;
    private final Queue<ConnectionRequest>    connectionRequests = new LinkedList<ClientConnectionEstablisher.ConnectionRequest>();
    private Thread                            connectionEstablisherThread;

    public AsyncReconnect(ClientConnectionEstablisher cce) {
      this.cce = cce;
    }

    public boolean isStopped() {
      return stopped;
    }

    private void awaitTermination(final boolean mayInterruptIfRunning) {
      synchronized (this) {
        connectionRequests.clear();
      }
      LOGGER.info("waiting for connection establisher to finish " + connectionEstablisherThread);
      boolean isInterrupted = false;
      try {
        if (Thread.currentThread() != connectionEstablisherThread && connectionEstablisherThread != null) {
          if (mayInterruptIfRunning) {
            connectionEstablisherThread.interrupt();
          }
          connectionEstablisherThread.join();
        }
      } catch (InterruptedException e) {
        LOGGER.warn("Got interrupted while waiting for connectionEstablisherThread to complete");
        isInterrupted = true;
      } finally {
        Util.selfInterruptIfNeeded(isInterrupted);
      }
    }

    public synchronized void stop() {
      stopped = true;
      this.notifyAll();
    }

    public synchronized void putConnectionRequest(ConnectionRequest request) {
      connectionRequests.add(request);
      this.notifyAll();
    }

    // for testing only
    synchronized int connectionRequestQueueSize() {
      return connectionRequests.size();
    }

    public synchronized ConnectionRequest waitUntilRequestAvailableOrStopped() {
      boolean isInterrupted = false;
      try {
        while (!stopped && connectionRequests.isEmpty()) {
          try {
            this.wait();
          } catch (InterruptedException e) {
            isInterrupted = true;
          }
        }
        return connectionRequests.poll();
      } finally {
        if (isInterrupted) {
          Util.selfInterruptIfNeeded(isInterrupted);
        }
      }
    }

    public void startThreadIfNecessary() {
      if (threadStarted.compareAndSet(false, true)) {
        Thread thread = new Thread(this, RECONNECT_THREAD_NAME + "-" + cce.connAddressProvider.getGroupId());
        thread.setDaemon(true);
        thread.start();
        connectionEstablisherThread = thread;
      }
    }

    @Override
    public void run() {
      while (!stopped) {
        ConnectionRequest request = waitUntilRequestAvailableOrStopped();
        if (stopped) {
          break;
        }
        if (request == null) { throw new AssertionError("AsyncReconnect not stopped yet, but next request was null"); }

        logger.info("Handling connection request: " + request);
        ClientMessageTransport cmt = request.getClientMessageTransport();
        try {
          switch (request.getType()) {
            case RECONNECT:
              this.cce.reconnect(cmt);
              break;
            case RESTORE_CONNECTION:
              RestoreConnectionRequest req = (RestoreConnectionRequest) request;
              this.cce.restoreConnection(req.getClientMessageTransport(), req.getSocketAddress(),
                                         req.getTimeoutMillis(), req.getCallback());
              break;
            default:
              throw new AssertionError("Unknown connection request type - " + request.getType());
          }
        } catch (MaxConnectionsExceededException e) {
          String connInfo = ((cmt == null) ? "" : (cmt.getLocalAddress() + "->" + cmt.getRemoteAddress() + " "));
          CustomerLogging.getConsoleLogger().fatal(connInfo + e.getMessage());
          if (cmt != null) cmt.getLogger().warn("No longer trying to reconnect.");
          return;
        } catch (Throwable t) {
          if (cmt != null) cmt.getLogger().warn("Reconnect failed !", t);
        }
      }
      logger.info("Connection establisher exiting.");
    }
  }

  static enum ConnectionRequestType {
    RECONNECT, RESTORE_CONNECTION;
  }

  static class ConnectionRequest {

    private final ConnectionRequestType  type;
    private final ClientMessageTransport cmt;

    public ConnectionRequest(ConnectionRequestType requestType) {
      this(requestType, null);
    }

    public ConnectionRequest(ConnectionRequestType requestType, ClientMessageTransport cmt) {
      this.cmt = cmt;
      this.type = requestType;
    }

    public ConnectionRequestType getType() {
      return type;
    }

    public ClientMessageTransport getClientMessageTransport() {
      return this.cmt;
    }

    public static ConnectionRequest newReconnectRequest(final ClientMessageTransport cmtParam) {
      return new ConnectionRequest(ConnectionRequestType.RECONNECT, cmtParam);
    }

    public static ConnectionRequest newRestoreConnectionRequest(final ClientMessageTransport cmtParam,
                                                                final TCSocketAddress sa,
                                                                final RestoreConnectionCallback callback,
                                                                final long timeoutMillis) {
      return new RestoreConnectionRequest(cmtParam, sa, callback, timeoutMillis);
    }

    @Override
    public String toString() {
      return "ConnectionRequest [type=" + type + ", cmt=" + cmt + "]";
    }
  }

  static class RestoreConnectionRequest extends ConnectionRequest {

    private final RestoreConnectionCallback callback;
    private final long                      timeoutMillis;
    private final TCSocketAddress           sa;

    public RestoreConnectionRequest(ClientMessageTransport cmt, final TCSocketAddress sa,
                                    RestoreConnectionCallback callback, long timeoutMillis) {
      super(ConnectionRequestType.RESTORE_CONNECTION, cmt);
      this.callback = callback;
      this.timeoutMillis = timeoutMillis;
      this.sa = sa;
    }

    public TCSocketAddress getSocketAddress() {
      return this.sa;
    }

    public RestoreConnectionCallback getCallback() {
      return this.callback;
    }

    public long getTimeoutMillis() {
      return this.timeoutMillis;
    }

    @Override
    public String toString() {
      return "RestoreConnectionRequest [type=" + getType() + ", clientMessageTransport=" + getClientMessageTransport()
             + ", callback=" + callback + ", timeoutMillis=" + timeoutMillis + ", sa=" + sa + "]";
    }

  }
}
