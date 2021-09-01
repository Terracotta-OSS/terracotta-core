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
import com.tc.net.protocol.NetworkStackID;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.CompositeIterator;
import com.tc.util.TCTimeoutException;
import com.tc.util.Util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

/**
 * This guy establishes a connection to the server for the Client.
 */
public class ClientConnectionEstablisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionEstablisher.class);

  private static final long                 CONNECT_RETRY_INTERVAL;
  private static final long                 MIN_RETRY_INTERVAL    = 1000;
  public static final String                RECONNECT_THREAD_NAME = "ConnectionEstablisher";

  private volatile Iterable<InetSocketAddress>       serverAddresses;
  private final Set<InetSocketAddress>      redirects = new LinkedHashSet<>();
  private final ClientMessageTransport transport;
  //  tie these two variables in synchronized blocks
  private boolean               allowReconnects       = false;
  private AsyncReconnect        asyncReconnect;
    
  static {
    Logger logger = LoggerFactory.getLogger(ClientConnectionEstablisher.class);
    long value = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.L1_SOCKET_RECONNECT_WAIT_INTERVAL);
    if (value < MIN_RETRY_INTERVAL) {
      logger.info("Forcing reconnect wait interval to " + MIN_RETRY_INTERVAL + " (configured value was " + value + ")");
      value = MIN_RETRY_INTERVAL;
    }

    CONNECT_RETRY_INTERVAL = value;
  }

  public ClientConnectionEstablisher(ClientMessageTransport cmt) {
    this.transport = cmt;
  }
  /**
   * Blocking open. Causes a connection to be made. Will throw exceptions if the connect fails.
   * 
   * @throws TCTimeoutException
   * @throws IOException
   * @throws CommStackMismatchException
   * @throws MaxConnectionsExceededException
   */
  public NetworkStackID open(Iterable<InetSocketAddress> serverAddresses,
                             ClientConnectionErrorListener reporter) throws TCTimeoutException, IOException, MaxConnectionsExceededException,
      CommStackMismatchException {
    Assert.assertNotNull(transport);
    Assert.assertNotNull(reporter);
    Assert.assertNotNull(serverAddresses);
    Assert.assertFalse(transport.wasOpened());

    if (enableReconnects()) {
      setServerAddresses(serverAddresses);
      try {
        return connectTryAllOnce(reporter);
      } catch (CommStackMismatchException |
              IOException |
              MaxConnectionsExceededException |
              TCTimeoutException |
              RuntimeException e) {
        AsyncReconnect reconnect = disableReconnects();
        // if there was an error here, no reconnect attempts should have happened, thus no thread created
        Assert.assertNull(reconnect.getConnectionThread());
        throw e;
      }
    } else {
      throw new IOException("connection already opened");
    }
  }

  void shutdown() {
    this.quitReconnectAttempts();
    this.transport.close();
  }

  private void setServerAddresses(Iterable<InetSocketAddress> serverAddresses) {
    this.serverAddresses = serverAddresses;
  }

  NetworkStackID connectTryAllOnce(ClientConnectionErrorListener reporter) throws TCTimeoutException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Assert.assertFalse(transport.isConnected());
    Iterator<InetSocketAddress> serverAddressIterator = getServerAddressIterator();
    InetSocketAddress target = null;
    
    while (target != null || serverAddressIterator.hasNext()) {
      if (target == null) {
        target = nextServerAddress(serverAddressIterator);
      }
      try {
        return transport.open(target);
      } catch (TransportRedirect redirect) {
        reporter.onError(target, redirect);
        target = InetSocketAddress.createUnresolved(redirect.getHostname(), redirect.getPort());
        redirects.add(target);
      } catch (NoActiveException noactive) {
        reporter.onError(target, noactive);
        target = null;
        LOGGER.debug("Connection attempt failed: ", noactive);
        // if there is no active, throw an IOException and let upper layers of 
        // the network stack handle the issue
        if (!serverAddressIterator.hasNext()) { throw new IOException(noactive); }
      } catch (TCTimeoutException | IOException e) {
        reporter.onError(target, e);
        target = null;
        if (!serverAddressIterator.hasNext()) { throw e; }
      }
    }
    throw new IOException("no connection target available");
  }

  @Override
  public String toString() {
    return "ClientConnectionEstablisher[" + this.serverAddresses + "]";
  }

  void reconnect(Supplier<Boolean> stopCheck) throws MaxConnectionsExceededException {
    try {
      // Lossy logging for connection errors. Log the errors once in every 10 seconds
      LossyTCLogger connectionErrorLossyLogger = new LossyTCLogger(transport.getLogger(), 10000,
                                                                   LossyTCLoggerType.TIME_BASED, true);

      if (!transport.getProductID().isReconnectEnabled() && !transport.isRetryOnReconnectionRejected()) {
        transport.getLogger().info("Got reconnect request for ClientMessageTransport that does not support it.  skipping");
        return;
      } else {
        transport.getLogger().info("reconnecting " + transport.getConnectionID());
      }

      boolean connected = transport.isConnected();
      boolean reconnectionRejected = false;
      InetSocketAddress target = null;

      for (int i = 0; tryToConnect(connected, stopCheck); i++) {
        Iterator<InetSocketAddress> serverAddressIterator = getServerAddressIterator();
        while ((target != null || serverAddressIterator.hasNext()) && tryToConnect(connected, stopCheck)) {

          if (reconnectionRejected) {
            if (transport.isRetryOnReconnectionRejected()) {
              LOGGER.info("Reconnection rejected by L2, trying again to reconnect - " + transport);
            } else {
              LOGGER.info("Reconnection rejected by L2, no more trying to reconnect - " + transport);
              return;
            }
          }

          if (target == null) {
            target = nextServerAddress(serverAddressIterator);
          }

          // DEV-1945
          if (i == 0) {
            String previousConnectHost = "";
            int previousConnectHostPort = -1;
            if (transport.getRemoteAddress() != null) {
              previousConnectHost = transport.getRemoteAddress().getAddress().getHostAddress();
              previousConnectHostPort = transport.getRemoteAddress().getPort();
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
            if ((serverAddressIterator.hasNext()) && (previousConnectHost.equals(connectingToHost))
                && (previousConnectHostPort == connectingToHostPort)) {
              target = null;
              continue;
            }
          }
          try {
            if (i % 20 == 0 && i > 0) {
              transport.getLogger().info("Reconnect attempt " + i + " to " + target);
            }
            transport.reopen(target);
            connected = transport.isConnected() && transport.getConnectionID().isValid();
          } catch (TransportRedirect redirect) {
            target = InetSocketAddress.createUnresolved(redirect.getHostname(), redirect.getPort());
          } catch (NoActiveException noactive) {
            target = null;
            handleConnectException(new IOException(noactive), false, connectionErrorLossyLogger);
          } catch (MaxConnectionsExceededException e) {
            target = null;
            quitReconnectAttempts();
            throw e;
          } catch (ReconnectionRejectedException e) {
            target = null;
            quitReconnectAttempts();
            reconnectionRejected = true;
            handleConnectException(e, false, connectionErrorLossyLogger);
          } catch (CommStackMismatchException e) {
            target = null;
            quitReconnectAttempts();
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
          } catch (Throwable t) {
            target = null;
            LOGGER.warn("unknown error", t);
          }
        }
      }
    } finally {
      LOGGER.info("reconnection complete connected:" + transport.isConnected());
    }
  }

  private Iterator<InetSocketAddress> getServerAddressIterator() {
    return new CompositeIterator<>(asList(serverAddresses.iterator(), new LinkedHashSet<>(redirects).iterator()));
  }

  private static InetSocketAddress nextServerAddress(Iterator<InetSocketAddress> serverAddressIterator) {
    InetSocketAddress serverAddress = serverAddressIterator.next();
    if (serverAddress.getPort() <= 0) {
      serverAddress = InetSocketAddress.createUnresolved(serverAddress.getHostString(), 9410);
    }
    return serverAddress;
  }

  String getHostByName(InetSocketAddress serverAddress) throws UnknownHostException {
    return InetAddress.getByName(serverAddress.getHostName()).getHostAddress();
  }
  
  private boolean tryToConnect(boolean connected, Supplier<Boolean> stopCheck) {
    boolean stopper = stopCheck.get();
    boolean stopped = !isReconnectEnabled();
    LOGGER.debug("trying to connect connected:{} stopCheck: {} isStopped: {}", connected, stopper, stopped);
    return !connected && !stopper && !stopped;
  }

  void handleConnectException(Exception e, boolean logFullException, Logger logger) {
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

  public boolean asyncReconnect(Supplier<Boolean> stopCheck) {
    if (transport.getConnectionID().isValid()) {
      LOGGER.info("async reconnect initiated " + transport.getConnectionID());
      return putConnectionRequest(ConnectionRequest.newReconnectRequest(stopCheck));
    } else {
      LOGGER.info("async reconnect ignored connection not valid " + transport.getConnectionID());
      transport.close();
      return false;
    }
  }

  private boolean putConnectionRequest(ConnectionRequest request) {
    AsyncReconnect reconnect = getReconnectHandler();
    if (reconnect == null || reconnect.isStopped()) {
      LOGGER.info("Ignoring connection request: " + request + " as allowReconnects: " + (reconnect == null)
                  + ", asyncReconnect.isStopped(): " + reconnect.isStopped());
      return false;
    }

    // Allow the async thread reconnects/restores only when cmt was connected atleast once
    if (transport != null) {
      if (transport.isConnected()) {
        LOGGER.info("Ignoring connection request.  The connection is already open. {}", transport);
      } else if (!transport.wasOpened()) {
        LOGGER.info("Ignoring connection request as transport was not connected even once");
      } else {
        return reconnect.putConnectionRequest(request);
      }
    } else {
      LOGGER.warn("no transport {}", request);
    }
    return false;
  }

  private synchronized boolean enableReconnects() {
    //  reconnects need to be disabled and any previous thread complete
    if (!allowReconnects) {
      allowReconnects = true;
      Assert.assertTrue(asyncReconnect == null);
      asyncReconnect = new AsyncReconnect();
      return true;
    } else {
      return false;
    }
  }

  private synchronized AsyncReconnect disableReconnects() {
    try {
      allowReconnects = false;
      return asyncReconnect;
    } finally {
      asyncReconnect = null;
    }
  }

  public synchronized boolean isReconnectEnabled() {
    // both conditions should always match.
    return allowReconnects && asyncReconnect != null;
  }
  
  private synchronized AsyncReconnect getReconnectHandler() {
    if (allowReconnects) {
      Assert.assertNotNull(asyncReconnect);
      return asyncReconnect;
    } else {
      return null;
    }
  }

  private void quitReconnectAttempts() {
    AsyncReconnect reconnect = disableReconnects();
    if (reconnect != null) {
      reconnect.stop();
      if (!transport.isRetryOnReconnectionRejected()) {
        reconnect.awaitTermination(true);
      }
    }
  }

  // for testing only
  void waitForTermination() {
    AsyncReconnect reconnect = getReconnectHandler();
    if (reconnect != null) {
      reconnect.waitForThreadToComplete();
    }
  }

  private class AsyncReconnect {
    private boolean stopped = false;
    private Thread connectionEstablisherThread;

    /**
     *
     * @param oldThread thread to wait for if not current thread
     * @param mayInterruptIfRunning interrupt thread
     * @return true if the passed in thread is joined
     */
    private boolean waitForThread(Thread oldThread, boolean mayInterruptIfRunning) {
      boolean isInterrupted = false;
      try {
        if (oldThread == null) {
          return true;
        } else if (Thread.currentThread() != oldThread) {
          if (mayInterruptIfRunning) {
            oldThread.interrupt();
          }
          oldThread.join();
          return true;
        }
      } catch (InterruptedException e) {
        LOGGER.info("Got interrupted while waiting for connectionEstablisherThread to complete");
        isInterrupted = true;
      } finally {
        Util.selfInterruptIfNeeded(isInterrupted);
      }
      return false; //  even if interrupted, don't reschedule
    }

    // for testing only
    private boolean waitForThreadToComplete() {
      return waitForThread(getConnectionThread(), false);
    }

    private void awaitTermination(boolean mayInterruptIfRunning) {
      Assert.assertTrue(isStopped());
      waitForThread(getConnectionThread(), mayInterruptIfRunning);
    }

    public synchronized boolean isStopped() {
      return stopped;
    }
    
    public synchronized void stop() {
      LOGGER.debug("Connection establisher stopping for connection {} to {}", transport.getConnectionID(), serverAddresses);
      stopped = true;
      this.notifyAll();
    }
    
    private synchronized Thread getConnectionThread() {
      return connectionEstablisherThread;
    }

    public boolean putConnectionRequest(ConnectionRequest request) {
      if (!isStopped() && waitForThread(getConnectionThread(), true)) {
        if (!transport.isConnected()) {
          startThreadIfNecessary(request);
          return true;
        } else {
          LOGGER.info("ignoring connection request.  Already connected: {}", transport);
        }
      } else {
        LOGGER.info("connect request ignored, already reconnecting");
      }
      return false;
    }

    private synchronized void startThreadIfNecessary(ConnectionRequest request) {
      if (!stopped) {
        Thread thread = new Thread(()->execute(request), RECONNECT_THREAD_NAME + "-" + serverAddresses.toString() + "-" + transport.getConnectionID());
        thread.setDaemon(true);
        thread.start();
        LOGGER.info("connection establisher started " + thread.getName());
        connectionEstablisherThread = thread;
      }
    }

    public void execute(ConnectionRequest request) {
      LOGGER.info("reconnection starting for connection {} to {}", transport.getConnectionID(), serverAddresses);
      if (request != null) {
        Logger clientLogger = transport.getLogger();
        try {
          reconnect(()->(isStopped() || request.checkForStop()));
        } catch (MaxConnectionsExceededException e) {
          String connInfo = ((transport == null) ? "" : (transport.getLocalAddress() + "->" + transport.getRemoteAddress() + " "));
          transport.getLogger().error(connInfo + e.getMessage());
        } catch (Throwable t) {
          if (transport != null) clientLogger.warn("Reconnect failed !", t);
        }
      }
      LOGGER.info("reconnection exiting for connection {} to {}", transport.getConnectionID(), serverAddresses);
    }
  }

  static class ConnectionRequest {
    private final Supplier<Boolean> stopCheck;

    ConnectionRequest(Supplier<Boolean> stopCheck) {
      this.stopCheck = stopCheck;
    }
    
    boolean checkForStop() {
      return stopCheck.get();
    }

    public static ConnectionRequest newReconnectRequest(Supplier<Boolean> stopCheck) {
      return new ConnectionRequest(stopCheck);
    }
  }
}
