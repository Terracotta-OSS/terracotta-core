/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.logging.TCLogger;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressIterator;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.IOException;

/**
 * This guy establishes a connection to the server for the Client.
 */
public class ClientConnectionEstablisher {

  private static final long               CONNECT_RETRY_INTERVAL = 1000;

  private final String                    desc;
  private final int                       maxReconnectTries;
  private final int                       timeout;
  private final ConnectionAddressProvider connAddressProvider;
  private final TCConnectionManager       connManager;

  private final SynchronizedBoolean       connecting             = new SynchronizedBoolean(false);

  private Thread                          connectionEstablisher;

  private NoExceptionLinkedQueue          reconnectRequest       = new NoExceptionLinkedQueue();  // <ConnectionRequest>

  public ClientConnectionEstablisher(TCConnectionManager connManager, ConnectionAddressProvider connAddressProvider,
                                     int maxReconnectTries, int timeout) {
    this.connManager = connManager;
    this.connAddressProvider = connAddressProvider;
    this.maxReconnectTries = maxReconnectTries;
    this.timeout = timeout;

    if (maxReconnectTries == 0) desc = "none";
    else if (maxReconnectTries < 0) desc = "unlimited";
    else desc = "" + maxReconnectTries;

  }

  /**
   * Blocking open. Causes a connection to be made. Will throw exceptions if the connect fails.
   * 
   * @throws TCTimeoutException
   * @throws IOException
   * @throws TCTimeoutException
   * @throws MaxConnectionsExceededException
   */
  public TCConnection open(ClientMessageTransport cmt) throws TCTimeoutException, IOException {
    synchronized (connecting) {
      Assert.eval("Can't call open() concurrently", !connecting.get());
      connecting.set(true);

      try {
        return connectTryAllOnce(cmt);
      } finally {
        connecting.set(false);
      }
    }
  }

  private TCConnection connectTryAllOnce(ClientMessageTransport cmt) throws TCTimeoutException, IOException {
    final ConnectionAddressIterator addresses = connAddressProvider.getIterator();
    TCConnection rv = null;
    while (addresses.hasNext()) {
      final ConnectionInfo connInfo = addresses.next();
      try {
        final TCSocketAddress csa = new TCSocketAddress(connInfo);
        rv = connect(csa, cmt);
        break;
      } catch (TCTimeoutException e) {
        if (!addresses.hasNext()) { throw e; }
      } catch (IOException e) {
        if (!addresses.hasNext()) { throw e; }
      }
    }
    return rv;
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
    connection.connect(sa, timeout);
    return connection;
  }

  public String toString() {
    return "ClientConnectionEstablisher[" + connAddressProvider + ", timeout=" + timeout + "]";
  }

  private void reconnect(ClientMessageTransport cmt) throws MaxConnectionsExceededException {
    try {
      boolean connected = false;
      for (int i = 0; ((maxReconnectTries < 0) || (i < maxReconnectTries)) && !connected; i++) {
        ConnectionAddressIterator addresses = connAddressProvider.getIterator();
        while (addresses.hasNext() && !connected) {
          final ConnectionInfo connInfo = addresses.next();
          try {
            if (i % 20 == 0) {
              cmt.logger.warn("Reconnect attempt " + i + " of " + desc + " reconnect tries to " + connInfo
                              + ", timeout=" + timeout);
            }
            TCConnection connection = connect(new TCSocketAddress(connInfo), cmt);
            cmt.reconnect(connection);
            connected = true;
          } catch (MaxConnectionsExceededException e) {
            throw e;
          } catch (TCTimeoutException e) {
            handleConnectException(e, false, cmt.logger);
          } catch (IOException e) {
            handleConnectException(e, false, cmt.logger);
          } catch (Exception e) {
            handleConnectException(e, true, cmt.logger);
          }
        }
      }
      cmt.endIfDisconnected();
    } finally {
      connecting.set(false);
    }
  }

  private void restoreConnection(ClientMessageTransport cmt, TCSocketAddress sa, long timeoutMillis,
                                 RestoreConnectionCallback callback) {
    final long deadline = System.currentTimeMillis() + timeoutMillis;
    boolean connected = false;
    for (int i = 0; true; i++) {
      try {
        TCConnection connection = connect(sa, cmt);
        cmt.reconnect(connection);
        connected = true;
      } catch (MaxConnectionsExceededException e) {
        // nothing
      } catch (TCTimeoutException e) {
        handleConnectException(e, false, cmt.logger);
      } catch (IOException e) {
        handleConnectException(e, false, cmt.logger);
      } catch (Exception e) {
        handleConnectException(e, true, cmt.logger);
      }
      if (connected || System.currentTimeMillis() > deadline) {
        break;
      }
    }
    connecting.set(false);
    if (!connected) {
      callback.restoreConnectionFailed(cmt);
    }
  }

  private void handleConnectException(Exception e, boolean logFullException, TCLogger logger) {
    if (logger.isDebugEnabled() || logFullException) {
      logger.error("Connect Exception", e);
    } else {
      logger.warn(e.getMessage());
    }
    try {
      Thread.sleep(CONNECT_RETRY_INTERVAL);
    } catch (InterruptedException e1) {
      //
    }
  }

  public void asyncReconnect(ClientMessageTransport cmt) {
    synchronized (connecting) {
      if (connecting.get()) return;
      putReconnectRequest(new ConnectionRequest(ConnectionRequest.RECONNECT, cmt));
    }
  }

  public void asyncRestoreConnection(ClientMessageTransport cmt, TCSocketAddress sa,
                                     RestoreConnectionCallback callback, long timeoutMillis) {
    synchronized (connecting) {
      if (connecting.get()) return;
      putReconnectRequest(new RestoreConnectionRequest(cmt, sa, callback, timeoutMillis));
    }
  }

  private void putReconnectRequest(ConnectionRequest request) {
    if (connectionEstablisher == null) {
      connecting.set(true);
      // First time
      connectionEstablisher = new Thread(new AsyncReconnect(this), "ConnectionEstablisher");
      connectionEstablisher.setDaemon(true);
      connectionEstablisher.start();

    }
    reconnectRequest.put(request);
  }

  public void quitReconnectAttempts() {
    putReconnectRequest(new ConnectionRequest(ConnectionRequest.QUIT, null));
  }

  static class AsyncReconnect implements Runnable {
    private final ClientConnectionEstablisher cce;

    public AsyncReconnect(ClientConnectionEstablisher cce) {
      this.cce = cce;
    }

    public void run() {
      ConnectionRequest request = null;
      while ((request = (ConnectionRequest) cce.reconnectRequest.take()) != null) {
        if (request.isReconnect()) {
          ClientMessageTransport cmt = request.getClientMessageTransport();
          try {
            cce.reconnect(cmt);
          } catch (MaxConnectionsExceededException e) {
            cmt.logger.warn(e);
            cmt.logger.warn("No longer trying to reconnect.");
            return;
          } catch (Throwable t) {
            cmt.logger.warn("Reconnect failed !", t);
          }
        } else if (request.isRestoreConnection()) {
          RestoreConnectionRequest req = (RestoreConnectionRequest) request;
          cce.restoreConnection(req.getClientMessageTransport(), req.getSocketAddress(), req.getTimeoutMillis(), req
              .getCallback());
        } else if (request.isQuit()) {
          break;
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
      return type == RECONNECT;
    }

    public boolean isQuit() {
      return type == QUIT;
    }

    public boolean isRestoreConnection() {
      return type == RESTORE_CONNECTION;
    }

    public TCSocketAddress getSocketAddress() {
      return sa;
    }

    public ClientMessageTransport getClientMessageTransport() {
      return cmt;
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
      return callback;
    }

    public long getTimeoutMillis() {
      return timeoutMillis;
    }
  }
}
