/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This guy establishes a connection to the server for the Client.
 */
public class ClientConnectionEstablisher {

  private static final long               CONNECT_RETRY_INTERVAL;
  private static final long               MIN_RETRY_INTERVAL    = 10;
  public static final String              RECONNECT_THREAD_NAME = "ConnectionEstablisher";

  private final String                    desc;
  private final int                       maxReconnectTries;
  private final int                       timeout;
  private final ConnectionAddressProvider connAddressProvider;
  private final TCConnectionManager       connManager;
  private final AtomicBoolean             asyncReconnecting     = new AtomicBoolean(false);
  private final AtomicBoolean             allowReconnects       = new AtomicBoolean(true);

  private Thread                          connectionEstablisher;
  private final NoExceptionLinkedQueue    reconnectRequest      = new NoExceptionLinkedQueue(); // <ConnectionRequest>

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
                                     int maxReconnectTries, int timeout) {
    this.connManager = connManager;
    this.connAddressProvider = connAddressProvider;
    this.maxReconnectTries = maxReconnectTries;
    this.timeout = timeout;

    if (maxReconnectTries == 0) {
      this.desc = "none";
    } else if (maxReconnectTries < 0) {
      this.desc = "unlimited";
    } else {
      this.desc = "" + maxReconnectTries;
    }

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
      connectTryAllOnce(cmt);
      this.allowReconnects.set(true);
    }
  }

  private void connectTryAllOnce(ClientMessageTransport cmt) throws TCTimeoutException, IOException,
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
  private TCConnection connect(TCSocketAddress sa, ClientMessageTransport cmt) throws TCTimeoutException, IOException {

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

  private void reconnect(ClientMessageTransport cmt) throws MaxConnectionsExceededException {
    try {
      // Lossy logging for connection errors. Log the errors once in every 10 seconds
      LossyTCLogger connectionErrorLossyLogger = new LossyTCLogger(cmt.logger, 10000, LossyTCLoggerType.TIME_BASED,
                                                                   true);

      boolean connected = cmt.isConnected();
      if (connected) {
        cmt.logger.warn("Got reconnect request for ClientMessageTransport that is connected.  skipping");
        return;
      }

      this.asyncReconnecting.set(true);
      for (int i = 0; ((this.maxReconnectTries < 0) || (i < this.maxReconnectTries)) && !connected; i++) {
        ConnectionAddressIterator addresses = this.connAddressProvider.getIterator();
        while (addresses.hasNext() && !connected) {

          if (cmt.isRejoinExpected()) {
            cmt.logger.warn("Skipping reconnect as it has been rejected. Expecting Rejoin.");
            return;
          }

          TCConnection connection = null;
          final ConnectionInfo connInfo = addresses.next();

          // DEV-1945
          if (i == 0) {
            String previousConnectHost = cmt.getRemoteAddress().getAddress().getHostAddress();
            String connectingToHost = "";
            try {
              connectingToHost = InetAddress.getByName(connInfo.getHostname()).getHostAddress();
            } catch (UnknownHostException e) {
              // these errors are caught even before
            }

            int previousConnectHostPort = cmt.getRemoteAddress().getPort();
            int connectingToHostPort = connInfo.getPort();

            if ((addresses.hasNext()) && (previousConnectHost.equals(connectingToHost))
                && (previousConnectHostPort == connectingToHostPort)) {
              continue;
            }
          }

          try {
            if (i % 20 == 0) {
              cmt.logger.warn("Reconnect attempt " + i + " of " + this.desc + " reconnect tries to " + connInfo
                              + ", timeout=" + this.timeout);
            }
            connection = connect(new TCSocketAddress(connInfo), cmt);
            cmt.reconnect(connection);
            connected = true;
          } catch (MaxConnectionsExceededException e) {
            throw e;
          } catch (ReconnectionRejectedException e) {
            handleConnectException(e, false, connectionErrorLossyLogger, connection);
          } catch (TCTimeoutException e) {
            handleConnectException(e, false, connectionErrorLossyLogger, connection);
          } catch (IOException e) {
            handleConnectException(e, false, connectionErrorLossyLogger, connection);
          } catch (Exception e) {
            handleConnectException(e, true, connectionErrorLossyLogger, connection);
          }

        }
      }
      cmt.endIfDisconnected();
    } finally {
      this.asyncReconnecting.set(false);
    }
  }

  private void restoreConnection(ClientMessageTransport cmt, TCSocketAddress sa, long timeoutMillis,
                                 RestoreConnectionCallback callback) throws MaxConnectionsExceededException {
    final long deadline = System.currentTimeMillis() + timeoutMillis;
    boolean connected = cmt.isConnected();
    if (connected) {
      cmt.logger.warn("Got restoreConnection request for ClientMessageTransport that is connected.  skipping");
    }

    this.asyncReconnecting.set(true);
    while (!connected) {

      if (cmt.isRejoinExpected()) {
        cmt.logger.warn("Skipping restore as it has been rejected. Expecting Rejoin.");
        return;
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
        handleConnectException(e, false, cmt.logger, connection);
      } catch (ReconnectionRejectedException e) {
        handleConnectException(e, false, cmt.logger, connection);
      } catch (IOException e) {
        handleConnectException(e, false, cmt.logger, connection);
      } catch (Exception e) {
        handleConnectException(e, true, cmt.logger, connection);
      }
      if (connected || System.currentTimeMillis() > deadline) {
        break;
      }
    }
    this.asyncReconnecting.set(false);
    if (!connected) {
      callback.restoreConnectionFailed(cmt);
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
    synchronized (this.asyncReconnecting) {
      if (this.asyncReconnecting.get()) { return; }
      putReconnectRequest(new ConnectionRequest(ConnectionRequest.RECONNECT, cmt));
    }
  }

  public void asyncRestoreConnection(ClientMessageTransport cmt, TCSocketAddress sa,
                                     RestoreConnectionCallback callback, long timeoutMillis) {
    synchronized (this.asyncReconnecting) {
      if (this.asyncReconnecting.get()) { return; }
      putReconnectRequest(new RestoreConnectionRequest(cmt, sa, callback, timeoutMillis));
    }
  }

  private void putReconnectRequest(ConnectionRequest request) {

    if (!this.allowReconnects.get()) { return; }

    if ((this.connectionEstablisher == null) && (!request.isQuit())) {
      // First time
      // Allow the async thread reconnects/restores only when cmt was connected atleast once
      if ((request.getClientMessageTransport() == null) || (!request.getClientMessageTransport().wasOpened())) { return; }

      this.connectionEstablisher = new Thread(new AsyncReconnect(this), RECONNECT_THREAD_NAME);
      this.connectionEstablisher.setDaemon(true);
      this.connectionEstablisher.start();
    }

    // DEV-1140 : avoiding the race condition
    // asyncReconnecting.set(true);
    this.reconnectRequest.put(request);
  }

  public void quitReconnectAttempts() {
    putReconnectRequest(new ConnectionRequest(ConnectionRequest.QUIT, null));
    this.allowReconnects.set(false);
  }

  static class AsyncReconnect implements Runnable {
    private final ClientConnectionEstablisher cce;

    public AsyncReconnect(ClientConnectionEstablisher cce) {
      this.cce = cce;
    }

    public void run() {
      ConnectionRequest request = null;
      while ((request = (ConnectionRequest) this.cce.reconnectRequest.take()) != null) {
        ClientMessageTransport cmt = request.getClientMessageTransport();
        try {
          if (request.isReconnect()) {
            this.cce.reconnect(cmt);
          } else if (request.isRestoreConnection()) {
            RestoreConnectionRequest req = (RestoreConnectionRequest) request;
            this.cce.restoreConnection(req.getClientMessageTransport(), req.getSocketAddress(), req.getTimeoutMillis(),
                                       req.getCallback());
          } else if (request.isQuit()) {
            break;
          }
        } catch (MaxConnectionsExceededException e) {
          String connInfo = ((cmt == null) ? "" : (cmt.getLocalAddress() + "->" + cmt.getRemoteAddress() + " "));
          CustomerLogging.getConsoleLogger().fatal(connInfo + e.getMessage());
          if (cmt != null) cmt.logger.warn("No longer trying to reconnect.");
          return;
        } catch (Throwable t) {
          if (cmt != null) cmt.logger.warn("Reconnect failed !", t);
        }
      }
    }
  }

  static class ConnectionRequest {

    public static final int              RECONNECT          = 1;
    public static final int              QUIT               = 2;
    public static final int              RESTORE_CONNECTION = 3;

    private final int                    type;
    private final TCSocketAddress        sa;
    private final ClientMessageTransport cmt;

    public ConnectionRequest(int type, ClientMessageTransport cmt) {
      this(type, cmt, null);
    }

    public ConnectionRequest(final int type, final ClientMessageTransport cmt, final TCSocketAddress sa) {
      this.type = type;
      this.cmt = cmt;
      this.sa = sa;
    }

    public boolean isReconnect() {
      return this.type == RECONNECT;
    }

    public boolean isQuit() {
      return this.type == QUIT;
    }

    public boolean isRestoreConnection() {
      return this.type == RESTORE_CONNECTION;
    }

    public TCSocketAddress getSocketAddress() {
      return this.sa;
    }

    public ClientMessageTransport getClientMessageTransport() {
      return this.cmt;
    }
  }

  static class RestoreConnectionRequest extends ConnectionRequest {

    private final RestoreConnectionCallback callback;
    private final long                      timeoutMillis;

    public RestoreConnectionRequest(ClientMessageTransport cmt, final TCSocketAddress sa,
                                    RestoreConnectionCallback callback, long timeoutMillis) {
      super(RESTORE_CONNECTION, cmt, sa);
      this.callback = callback;
      this.timeoutMillis = timeoutMillis;
    }

    public RestoreConnectionCallback getCallback() {
      return this.callback;
    }

    public long getTimeoutMillis() {
      return this.timeoutMillis;
    }
  }
}
