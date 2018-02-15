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
package com.tc.net.protocol.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.logging.LossyTCLogger;
import com.tc.logging.LossyTCLogger.LossyTCLoggerType;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.ReconnectionRejectedException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackID;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.Util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This guy establishes a connection to the server for the Client.
 */
public class ClientConnectionEstablisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionEstablisher.class);

  private static final long                 CONNECT_RETRY_INTERVAL;
  private static final long                 MIN_RETRY_INTERVAL    = 10;
  public static final String                RECONNECT_THREAD_NAME = "ConnectionEstablisher";

  private final LinkedHashSet<ConnectionInfo>   connAddressProvider;
  private final AtomicBoolean               asyncReconnecting     = new AtomicBoolean(false);
  private final AtomicBoolean               allowReconnects       = new AtomicBoolean(true);
  private volatile AsyncReconnect           asyncReconnect;

  private final ReconnectionRejectedHandler reconnectionRejectedHandler;

  static {
    Logger logger = LoggerFactory.getLogger(ClientConnectionEstablisher.class);
    long value = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.L1_SOCKET_RECONNECT_WAIT_INTERVAL);
    if (value < MIN_RETRY_INTERVAL) {
      logger.info("Forcing reconnect wait interval to " + MIN_RETRY_INTERVAL + " (configured value was " + value + ")");
      value = MIN_RETRY_INTERVAL;
    }

    CONNECT_RETRY_INTERVAL = value;
  }

  public ClientConnectionEstablisher(ReconnectionRejectedHandler reconnectionRejectedHandler) {
    this.connAddressProvider = new LinkedHashSet<ConnectionInfo>();
    this.reconnectionRejectedHandler = reconnectionRejectedHandler;
    this.asyncReconnect = new AsyncReconnect(this);
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
  void disableReconnectThreadSpawn() {
    this.asyncReconnect.disableThreadSpawn();
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
  public NetworkStackID open(Collection<ConnectionInfo> info, ClientMessageTransport cmt, ClientConnectionErrorListener reporter) throws TCTimeoutException, IOException, MaxConnectionsExceededException,
      CommStackMismatchException {
    Assert.assertNotNull(cmt);
    Assert.assertNotNull(reporter);
    Assert.assertNotNull(info);
    synchronized (this.asyncReconnecting) {
      if (info != null) {
        connAddressProvider.addAll(info);
      }
      Assert.eval("Can't call open() while asynch reconnect occurring", !this.asyncReconnecting.get());
      this.allowReconnects.set(true);
      return connectTryAllOnce(cmt, reporter);
    }
  }

  NetworkStackID connectTryAllOnce(ClientMessageTransport cmt, ClientConnectionErrorListener reporter) throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    final Iterator<ConnectionInfo> addresses = new ArrayList<ConnectionInfo>(this.connAddressProvider).iterator();
    Assert.assertFalse(cmt.isConnected());
    ConnectionInfo info = null;
    
    while (info != null || addresses.hasNext()) {
      if (info == null) {
        info = addresses.next();
      }
      try {
        return cmt.open(info);
      } catch (TransportRedirect redirect) {
        ConnectionInfo add = new ConnectionInfo(redirect.getHostname(), redirect.getPort());
        reporter.onError(info, redirect);
        info = null;
        if (this.connAddressProvider.add(add)) {
          info = add;
        }
      } catch (NoActiveException noactive) {
        reporter.onError(info, noactive);
        info = null;
        LOGGER.debug("Connection attempt failed: ", noactive);
        // if there is no active, throw an IOException and let upper layers of 
        // the network stack handle the issue
        throw new IOException(noactive);
      } catch (TCTimeoutException e) {
        reporter.onError(info, e);
        info = null;
        if (!addresses.hasNext()) { throw e; }
      } catch (IOException e) {
        reporter.onError(info, e);
        info = null;
        if (!addresses.hasNext()) { throw e; }
      }
    }
    throw new IOException("active not available");
  }

  @Override
  public String toString() {
    return "ClientConnectionEstablisher[" + this.connAddressProvider + "]";
  }

  void reconnect(ClientMessageTransport cmt) throws MaxConnectionsExceededException {
    try {
      // Lossy logging for connection errors. Log the errors once in every 10 seconds
      LossyTCLogger connectionErrorLossyLogger = new LossyTCLogger(cmt.getLogger(), 10000,
                                                                   LossyTCLoggerType.TIME_BASED, true);

      boolean connected = cmt.isConnected();
      if (!cmt.getConnectionId().getProductId().isReconnectEnabled() && !isReconnectBetweenL2s()) {
        cmt.getLogger().info("Got reconnect request for ClientMessageTransport that does not support it.  skipping");
        return;
      }

      this.asyncReconnecting.set(true);
      boolean reconnectionRejected = false;
      ConnectionInfo target = null;

      for (int i = 0; tryToConnect(connected); i++) {
        Iterator<ConnectionInfo> addresses = new ArrayList<ConnectionInfo>(this.connAddressProvider).iterator();
        while ((target != null || addresses.hasNext()) && tryToConnect(connected)) {

          if (reconnectionRejected) {
            if (reconnectionRejectedHandler.isRetryOnReconnectionRejected()) {
              LOGGER.info("Reconnection rejected by L2, trying again to reconnect - " + cmt);
            } else {
              LOGGER.info("Reconnection rejected by L2, no more trying to reconnect - " + cmt);
              return;
            }
          }

          if (target == null) {
            target = addresses.next();
          }

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
              connectingToHost = getHostByName(target);
            } catch (UnknownHostException e) {
              handleConnectException(e, true, connectionErrorLossyLogger);
              // keep trying reconnects, for maxReconnectTries
              target = null;
              continue;
            }
            int connectingToHostPort = target.getPort();
            if ((addresses.hasNext()) && (previousConnectHost.equals(connectingToHost))
                && (previousConnectHostPort == connectingToHostPort)) {
              target = null;
              continue;
            }
          }
          try {
            if (i % 20 == 0 && i > 0) {
              cmt.getLogger().info("Reconnect attempt " + i + " to " + target);
            }
            cmt.reopen(target);
            connected = cmt.getConnectionId().isValid();        
          } catch (TransportRedirect redirect) {
            ConnectionInfo add = new ConnectionInfo(redirect.getHostname(), redirect.getPort());
            target = null;
            if (this.connAddressProvider.add(add)) {
              target = add;
            }
          } catch (NoActiveException noactive) {
            target = null;
            handleConnectException(new IOException(noactive), false, connectionErrorLossyLogger);
          } catch (MaxConnectionsExceededException e) {
            target = null;
            reset();
            throw e;
          } catch (ReconnectionRejectedException e) {
            target = null;
            reset();
            reconnectionRejected = true;
            handleConnectException(e, false, connectionErrorLossyLogger);
          } catch (CommStackMismatchException e) {
            target = null;
            reset();
            handleConnectException(e, false, connectionErrorLossyLogger);
          } catch (TCTimeoutException e) {
            target = null;
            handleConnectException(e, true, connectionErrorLossyLogger);
          } catch (IOException e) {
            target = null;
            handleConnectException(e, false, connectionErrorLossyLogger);
          } catch (Exception e) {
            target = null;
            handleConnectException(e, true, connectionErrorLossyLogger);
          }
        }
      }
    } finally {
      asyncReconnecting.set(false);
    }
  }

  String getHostByName(ConnectionInfo connInfo) throws UnknownHostException {
    return InetAddress.getByName(connInfo.getHostname()).getHostAddress();
  }

  // TRUE for L2, for L1 only if not stopped
  boolean isReconnectBetweenL2s() {
    return reconnectionRejectedHandler.isRetryOnReconnectionRejected();
  }
  
  private boolean tryToConnect(boolean connected) {
    return !connected && !asyncReconnect.isStopped();
  }

  void restoreConnection(ClientMessageTransport cmt, TCSocketAddress sa, long timeoutMillis,
                         RestoreConnectionCallback callback) throws MaxConnectionsExceededException {
    final long deadline = System.currentTimeMillis() + timeoutMillis;
    boolean connected = cmt.isConnected();
    if (connected) {
      cmt.getLogger().info("Got restoreConnection request for ClientMessageTransport that is connected.  skipping");
      return;
    }

    this.asyncReconnecting.set(true);
    try {
      boolean reconnectionRejected = false;
      while (tryToConnect(connected)) {

        if (reconnectionRejected) {
          if (reconnectionRejectedHandler.isRetryOnReconnectionRejected()) {
            LOGGER.info("Reconnection rejected by L2, trying again to restore connection - " + cmt);
          } else {
            LOGGER.info("Reconnection rejected by L2, no more trying to restore connection - " + cmt);
            return;
          }
        }
        try {
          cmt.reconnect(sa);
          connected = true;
        } catch (TransportRedirect redirect) {
          Assert.fail();
        } catch (NoActiveException noactive) {
          Assert.fail();
        } catch (MaxConnectionsExceededException e) {
          callback.restoreConnectionFailed(cmt);
          reset();
          // DEV-2781
          throw e;
        } catch (TCTimeoutException e) {
          handleConnectException(e, true, cmt.getLogger());
        } catch (ReconnectionRejectedException e) {
          reconnectionRejected = true;
          reset();
          handleConnectException(e, false, cmt.getLogger());
        } catch (CommStackMismatchException e) {
          reset();
          handleConnectException(e, false, cmt.getLogger());
        } catch (IOException e) {
          handleConnectException(e, false, cmt.getLogger());
        } catch (Exception e) {
          handleConnectException(e, true, cmt.getLogger());
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

  private void handleConnectException(Exception e, boolean logFullException, Logger logger) {
    if (logger.isDebugEnabled() || logFullException) {
      logger.error("Connect Exception", e);
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
    if (cmt.getConnectionId().isValid()) {
      putConnectionRequest(ConnectionRequest.newReconnectRequest(cmt));
    }
  }

  public void asyncRestoreConnection(ClientMessageTransport cmt, TCSocketAddress sa,
                                     RestoreConnectionCallback callback, long timeoutMillis) {
    putConnectionRequest(ConnectionRequest.newRestoreConnectionRequest(cmt, sa, callback, timeoutMillis));
  }

  private void putConnectionRequest(ConnectionRequest request) {
    if (!this.allowReconnects.get() || asyncReconnect.isStopped()) {
      LOGGER.info("Ignoring connection request: " + request + " as allowReconnects: " + allowReconnects.get()
                  + ", asyncReconnect.isStopped(): " + asyncReconnect.isStopped());
      return;
    }
    // Allow the async thread reconnects/restores only when cmt was connected atleast once
    if (request.getClientMessageTransport() != null && request.getClientMessageTransport().wasOpened()) {
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
    private static final Logger logger = LoggerFactory.getLogger(AsyncReconnect.class);
    private final ClientConnectionEstablisher cce;
    private boolean                  stopped            = false;
    private final Queue<ConnectionRequest>    connectionRequests = new LinkedList<ConnectionRequest>();
    private Thread                            connectionEstablisherThread;
    private boolean                           disableThreadSpawn = false;

    public AsyncReconnect(ClientConnectionEstablisher cce) {
      this.cce = cce;
    }

    public synchronized boolean isStopped() {
      return stopped;
    }
    
    synchronized void disableThreadSpawn() {
      disableThreadSpawn = true;
    }
    
    private void waitForThread(Thread oldThread, boolean mayInterruptIfRunning) {
      boolean isInterrupted = false;
      try {
        if (Thread.currentThread() != oldThread && oldThread != null) {
          if (mayInterruptIfRunning) {
            oldThread.interrupt();
          }
          oldThread.join();
        }
      } catch (InterruptedException e) {
        LOGGER.info("Got interrupted while waiting for connectionEstablisherThread to complete");
        isInterrupted = true;
      } finally {
        Util.selfInterruptIfNeeded(isInterrupted);
      }
    }

    private void awaitTermination(boolean mayInterruptIfRunning) {
      synchronized (this) {
        if (!stopped) {
          throw new AssertionError("not stopped");
        }
        connectionRequests.clear();
        LOGGER.debug("waiting for connection establisher to finish " + connectionEstablisherThread);
        this.notifyAll();
      }
      waitForThread(connectionEstablisherThread, mayInterruptIfRunning);
    }

    public synchronized void stop() {
      logger.debug("Connection establisher stopping " + System.identityHashCode(this));
      stopped = true;
      this.notifyAll();
    }

    public synchronized void putConnectionRequest(ConnectionRequest request) {
      if (!stopped) {
        startThreadIfNecessary();
        connectionRequests.add(request);
        this.notifyAll();
      }
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

    private void startThreadIfNecessary() {
  //  Should be synchronized by caller
      if (connectionEstablisherThread == null && !disableThreadSpawn) {
        Thread thread = new Thread(this, RECONNECT_THREAD_NAME + "-" + this.cce.connAddressProvider.toString() + "-" + System.identityHashCode(this));
        thread.setDaemon(true);
        thread.start();
        connectionEstablisherThread = thread;
      }
    }

    @Override
    public void run() {
      logger.debug("Connection establisher starting. " + System.identityHashCode(this));
      while (!isStopped()) {
        ConnectionRequest request = waitUntilRequestAvailableOrStopped();
        if (request != null) {
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
            cmt.getLogger().error(connInfo + e.getMessage());
            return;
          } catch (Throwable t) {
            if (cmt != null) cmt.getLogger().warn("Reconnect failed !", t);
          }
        } else {
          if (!isStopped()) {
            throw new AssertionError("AsyncReconnect not stopped yet, but next request was null");
          }
        }
      }
      logger.info("Connection establisher exiting." + System.identityHashCode(this));
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

    public static ConnectionRequest newReconnectRequest(ClientMessageTransport cmtParam) {
      return new ConnectionRequest(ConnectionRequestType.RECONNECT, cmtParam);
    }

    public static ConnectionRequest newRestoreConnectionRequest(ClientMessageTransport cmtParam,
                                                                TCSocketAddress sa,
                                                                RestoreConnectionCallback callback,
                                                                long timeoutMillis) {
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

    public RestoreConnectionRequest(ClientMessageTransport cmt, TCSocketAddress sa,
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
