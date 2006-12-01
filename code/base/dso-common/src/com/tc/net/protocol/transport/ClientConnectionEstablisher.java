/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.logging.TCLogger;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This guy establishes a connection to the server for the Client.
 */
class ClientConnectionEstablisher implements Runnable, MessageTransportListener {

  private static final long               CONNECT_RETRY_INTERVAL = 1000;

  private static final Object             RECONNECT              = new Object();
  private static final Object             QUIT                   = new Object();

  private final ClientMessageTransport    transport;
  private final String                    desc;
  private final TCLogger                  logger;
  private final int                       maxReconnectTries;
  private final int                       timeout;
  private final ConnectionAddressProvider connAddressProvider;
  private final TCConnectionManager       connManager;

  private final SynchronizedBoolean       connecting             = new SynchronizedBoolean(false);

  private Thread                          connectionEstablisher;

  private NoExceptionLinkedQueue          reconnectRequest       = new NoExceptionLinkedQueue();

  ClientConnectionEstablisher(ClientMessageTransport transport, TCConnectionManager connManager,
                              ConnectionAddressProvider connAddressProvider, TCLogger logger, int maxReconnectTries,
                              int timeout) {
    this.transport = transport;
    this.connManager = connManager;
    this.logger = logger;
    this.connAddressProvider = connAddressProvider;
    this.maxReconnectTries = maxReconnectTries;
    this.timeout = timeout;

    if (maxReconnectTries == 0) desc = "none";
    else if (maxReconnectTries < 0) desc = "unlimited";
    else desc = "" + maxReconnectTries;

    this.transport.addTransportListener(this);
  }

  /**
   * Blocking open. Causes a connection to be made. Will throw exceptions if the connect fails.
   * 
   * @throws TCTimeoutException
   * @throws IOException
   * @throws TCTimeoutException
   * @throws MaxConnectionsExceededException
   */
  public void open() throws TCTimeoutException, IOException {
    synchronized (connecting) {
      Assert.eval("Can't call open() concurrently", !connecting.get());
      connecting.set(true);

      try {
        connectTryAllOnce();
      } finally {
        connecting.set(false);
      }
    }
  }

  private void connectTryAllOnce() throws TCTimeoutException, IOException {
    connAddressProvider.setPolicy(ConnectionAddressProvider.LINEAR);
    do {
      try {
        connect();
        return;
      } catch (TCTimeoutException e) {
        if (!connAddressProvider.hasNext()) { throw e; }
      } catch (IOException e) {
        if (!connAddressProvider.hasNext()) { throw e; }
      }
    } while (connAddressProvider.hasNext() && (connAddressProvider.next() != null));

  }

  /**
   * Tries to make a connection. This is a blocking call.
   * 
   * @throws TCTimeoutException
   * @throws IOException
   * @throws MaxConnectionsExceededException
   */
  void connect() throws TCTimeoutException, IOException {

    TCConnection connection = this.connManager.createConnection(transport.getProtocolAdapter());
    transport.wireNewConnection(connection);
    transport.fireTransportConnectAttemptEvent();
    TCSocketAddress address = createSocketAddress();
    connection.connect(address, timeout);
  }

  void disconnect() {
    transport.close();
  }

  private TCSocketAddress createSocketAddress() throws UnknownHostException {
    TCSocketAddress socketAddress = new TCSocketAddress(InetAddress.getByName(connAddressProvider.getHostname()),
                                                        connAddressProvider.getPortNumber());
    return socketAddress;
  }

  public String toString() {
    return "ClientConnectionEstablisher[" + connAddressProvider + ", timeout=" + timeout + "]";
  }

  public void reconnect() throws MaxConnectionsExceededException {

    try {
      boolean connected = false;
      connAddressProvider.setPolicy(ConnectionAddressProvider.ROUND_ROBIN);
      for (int i = 0; ((maxReconnectTries < 0) || (i < maxReconnectTries)) && !connected; i++) {
        for (int j = 0; j < connAddressProvider.getCount() && !connected; j++, connAddressProvider.next()) {
          // if (!this.transport.isOpen.get()) return;

          try {
            if (i % 20 == 0) {
              logger.warn("Reconnect attempt " + i + " of " + desc + " reconnect tries to "
                          + connAddressProvider.getConnectionInfo() + ", timeout=" + timeout);
            }
            connect();
            transport.reconnect();
            connected = true;
          } catch (MaxConnectionsExceededException e) {
            throw e;
          } catch (TCTimeoutException e) {
            handleConnectException(e, false);
          } catch (IOException e) {
            handleConnectException(e, false);
          } catch (Exception e) {
            handleConnectException(e, true);
          }
        }
      }

      transport.endIfDisconnected();

    } finally {
      connecting.set(false);
    }
  }

  private void handleConnectException(Exception e, boolean logFullException) {
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

  public void notifyTransportConnected(MessageTransport mt) {
    // NO OP
  }

  public void notifyTransportDisconnected(MessageTransport mt) {
    synchronized (connecting) {
      if (connecting.get()) return;

      if (connectionEstablisher == null) {
        connecting.set(true);
        // First time
        connectionEstablisher = new Thread(this, "ConnectionEstablisher");
        connectionEstablisher.setDaemon(true);
        connectionEstablisher.start();

      }
      reconnectRequest.put(RECONNECT);
    }
  }

  public void notifyTransportConnectAttempt(MessageTransport mt) {
    // NO OP
  }

  public void notifyTransportClosed(MessageTransport mt) {
    reconnectRequest.put(QUIT);
  }

  public void run() {
    Object request = null;
    while ((request = reconnectRequest.take()) != null) {
      if (request == RECONNECT) {
        try {
          reconnect();
        } catch (MaxConnectionsExceededException e) {
          logger.warn(e);
          logger.warn("No longer trying to reconnect.");
          return;
        } catch (Throwable t) {
          logger.warn("Reconnect failed !", t);
        }
      } else if (request == QUIT) {
        connectionEstablisher = null;
        break;
      }
    }
  }

}
