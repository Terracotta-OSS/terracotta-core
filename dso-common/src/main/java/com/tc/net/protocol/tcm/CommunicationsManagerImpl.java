/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.exception.TCRuntimeException;
import com.tc.license.ProductID;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.AddressChecker;
import com.tc.net.GroupID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.Constants;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.core.TCListener;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.transport.ConnectionHealthChecker;
import com.tc.net.protocol.transport.ConnectionHealthCheckerEchoImpl;
import com.tc.net.protocol.transport.ConnectionHealthCheckerImpl;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.DisabledHealthCheckerConfigImpl;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.net.protocol.transport.ReconnectionRejectedHandlerL1;
import com.tc.net.protocol.transport.ReconnectionRejectedHandlerL2;
import com.tc.net.protocol.transport.ServerMessageTransport;
import com.tc.net.protocol.transport.ServerStackProvider;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandler;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandlerForL1;
import com.tc.net.protocol.transport.TransportHandshakeMessage;
import com.tc.net.protocol.transport.TransportHandshakeMessageFactory;
import com.tc.net.protocol.transport.TransportMessageFactoryImpl;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactoryImpl;
import com.tc.net.protocol.transport.WireProtocolMessageSink;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Communications manager for setting up listeners and creating client connections
 * 
 * @author teck
 */
public class CommunicationsManagerImpl implements CommunicationsManager {
  private static final TCLogger                                             logger                    = TCLogging
                                                                                                          .getLogger(CommunicationsManager.class);

  private final SetOnceFlag                                                 shutdown                  = new SetOnceFlag();
  private final Set                                                         listeners                 = new HashSet();
  private final ReentrantLock                                               licenseLock               = new ReentrantLock();
  private final TCConnectionManager                                         connectionManager;
  private final boolean                                                     privateConnMgr;
  private final NetworkStackHarnessFactory                                  stackHarnessFactory;
  private final TransportHandshakeMessageFactory                            transportMessageFactory;
  private final MessageMonitor                                              monitor;
  private final TCMessageRouter                                             messageRouter;
  private final HealthCheckerConfig                                         healthCheckerConfig;
  private final ConnectionPolicy                                            connectionPolicy;
  private final ReconnectionRejectedHandler                                 reconnectionRejectedHandler;
  protected final ProductID productId;
  protected final ConcurrentHashMap<TCMessageType, Class>                   messageTypeClassMapping   = new ConcurrentHashMap<TCMessageType, Class>();
  protected final ConcurrentHashMap<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping = new ConcurrentHashMap<TCMessageType, GeneratedMessageFactory>();

  private ConnectionHealthChecker                                           connectionHealthChecker;
  private ServerID                                                          serverID                  = ServerID.NULL_ID;
  private int                                                               callbackPort              = TransportHandshakeMessage.NO_CALLBACK_PORT;
  private NetworkListener                                                   callbackportListener      = null;
  private final TransportHandshakeErrorHandler                              handshakeErrHandler;
  private final String                                                      commsMgrName;
  private final TCSecurityManager                                           securityManager;

  /**
   * Create a communications manager. This implies that one or more network handling threads will be started on your
   * behalf. As such, you should not be instantiating one of these per connection for instance.
   */
  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor,
                                   NetworkStackHarnessFactory stackHarnessFactory, ConnectionPolicy connectionPolicy) {
    this(commsMgrName, monitor, new TCMessageRouterImpl(), stackHarnessFactory, null, connectionPolicy, 0,
         new DisabledHealthCheckerConfigImpl(), new TransportHandshakeErrorHandlerForL1(), Collections.EMPTY_MAP,
         Collections.EMPTY_MAP, null);

  }

  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor,
                                   NetworkStackHarnessFactory stackHarnessFactory, ConnectionPolicy connectionPolicy,
                                   int workerCommCount) {
    this(commsMgrName, monitor, new TCMessageRouterImpl(), stackHarnessFactory, null, connectionPolicy,
         workerCommCount, new DisabledHealthCheckerConfigImpl(), new TransportHandshakeErrorHandlerForL1(),
         Collections.EMPTY_MAP, Collections.EMPTY_MAP, null);
  }

  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor,
                                   NetworkStackHarnessFactory stackHarnessFactory, ConnectionPolicy connectionPolicy,
                                   HealthCheckerConfig config) {
    this(commsMgrName, monitor, new TCMessageRouterImpl(), stackHarnessFactory, connectionPolicy, config,
         Collections.EMPTY_MAP, Collections.EMPTY_MAP);
  }

  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor, TCMessageRouter messageRouter,
                                   NetworkStackHarnessFactory stackHarnessFactory, ConnectionPolicy connectionPolicy,
                                   HealthCheckerConfig config, Map<TCMessageType, Class> messageTypeClassMapping,
                                   Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping) {
    this(commsMgrName, monitor, messageRouter, stackHarnessFactory, null, connectionPolicy, 0, config,
         new TransportHandshakeErrorHandlerForL1(), messageTypeClassMapping, messageTypeFactoryMapping, null);
  }

  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor, TCMessageRouter messageRouter,
                                   NetworkStackHarnessFactory stackHarnessFactory, ConnectionPolicy connectionPolicy,
                                   int workerCommCount, HealthCheckerConfig config, ServerID serverID,
                                   TransportHandshakeErrorHandler transportHandshakeErrorHandler,
                                   Map<TCMessageType, Class> messageTypeClassMapping,
                                   Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping,
                                   TCSecurityManager securityManager) {
    this(commsMgrName, monitor, messageRouter, stackHarnessFactory, null, connectionPolicy, workerCommCount, config,
         transportHandshakeErrorHandler, messageTypeClassMapping, messageTypeFactoryMapping,
         ReconnectionRejectedHandlerL2.SINGLETON, securityManager, null);
    this.serverID = serverID;
  }

  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor, TCMessageRouter messageRouter,
                                   NetworkStackHarnessFactory stackHarnessFactory, TCConnectionManager connMgr,
                                   ConnectionPolicy connectionPolicy, int workerCommCount,
                                   HealthCheckerConfig healthCheckerConf,
                                   TransportHandshakeErrorHandler transportHandshakeErrorHandler,
                                   Map<TCMessageType, Class> messageTypeClassMapping,
                                   Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping,
                                   TCSecurityManager securityManager) {
    this(commsMgrName, monitor, messageRouter, stackHarnessFactory, connMgr, connectionPolicy, workerCommCount,
         healthCheckerConf, transportHandshakeErrorHandler, messageTypeClassMapping, messageTypeFactoryMapping,
         ReconnectionRejectedHandlerL1.SINGLETON, securityManager, null);
  }

  /**
   * Create a comms manager with the given connection manager. This cstr is mostly for testing, or in the event that you
   * actually want to use an explicit connection manager
   *
   * @param connMgr the connection manager to use
   * @param productId
   */
  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor, TCMessageRouter messageRouter,
                                   NetworkStackHarnessFactory stackHarnessFactory, TCConnectionManager connMgr,
                                   ConnectionPolicy connectionPolicy, int workerCommCount,
                                   HealthCheckerConfig healthCheckerConf,
                                   TransportHandshakeErrorHandler transportHandshakeErrorHandler,
                                   Map<TCMessageType, Class> messageTypeClassMapping,
                                   Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping,
                                   ReconnectionRejectedHandler reconnectionRejectedHandler, TCSecurityManager securityManager,
                                   ProductID productId) {
    this.commsMgrName = commsMgrName;
    this.monitor = monitor;
    this.messageRouter = messageRouter;
    this.productId = productId;
    this.transportMessageFactory = new TransportMessageFactoryImpl();
    this.connectionPolicy = connectionPolicy;
    this.stackHarnessFactory = stackHarnessFactory;
    this.healthCheckerConfig = healthCheckerConf;
    this.handshakeErrHandler = transportHandshakeErrorHandler;
    this.privateConnMgr = (connMgr == null);
    this.messageTypeClassMapping.putAll(messageTypeClassMapping);
    this.messageTypeFactoryMapping.putAll(messageTypeFactoryMapping);
    this.reconnectionRejectedHandler = reconnectionRejectedHandler;
    this.securityManager = securityManager;

    Assert.assertNotNull(commsMgrName);
    if (null == connMgr) {
      this.connectionManager = new TCConnectionManagerImpl(commsMgrName, workerCommCount, healthCheckerConfig, securityManager);
    } else {
      this.connectionManager = connMgr;
    }

    Assert.eval(healthCheckerConfig != null);
    if (healthCheckerConfig.isHealthCheckerEnabled()) {
      connectionHealthChecker = new ConnectionHealthCheckerImpl(healthCheckerConfig, connectionManager);
    } else {
      connectionHealthChecker = new ConnectionHealthCheckerEchoImpl();
    }
    connectionHealthChecker.start();
    startHealthCheckCallbackPortListener(healthCheckerConfig);
  }

  @Override
  public TCConnectionManager getConnectionManager() {
    return this.connectionManager;
  }

  @Override
  public boolean isInShutdown() {
    return shutdown.isSet();
  }

  @Override
  public void shutdown() {
    if (shutdown.attemptSet()) {
      connectionHealthChecker.stop();
      if (privateConnMgr) {
        connectionManager.shutdown();
      }
    } else {
      logger.warn("shutdown already started");
    }
  }

  @Override
  public NetworkListener[] getAllListeners() {
    synchronized (listeners) {
      return (NetworkListener[]) listeners.toArray(new NetworkListener[listeners.size()]);
    }
  }

  @Override
  public void addClassMapping(TCMessageType messageType, Class messageClass) {
    messageTypeClassMapping.put(messageType, messageClass);
  }

  @Override
  public void addClassMapping(TCMessageType messageType, GeneratedMessageFactory generatedMessageFactory) {
    messageTypeFactoryMapping.put(messageType, generatedMessageFactory);
  }

  @Override
  public ClientMessageChannel createClientChannel(final SessionProvider sessionProvider, final int maxReconnectTries,
                                                  String hostname, int port, final int timeout,
                                                  ConnectionAddressProvider addressProvider) {
    return createClientChannel(sessionProvider, maxReconnectTries, hostname, port, timeout, addressProvider, null, null);

  }

  @Override
  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, final int maxReconnectTries,
                                                  String hostname, int port, final int timeout,
                                                  ConnectionAddressProvider addressProvider,
                                                  MessageTransportFactory transportFactory) {
    return createClientChannel(sessionProvider, maxReconnectTries, hostname, port, timeout, addressProvider,
                               transportFactory, null);
  }

  @Override
  public ClientMessageChannel createClientChannel(final SessionProvider sessionProvider, final int maxReconnectTries,
                                                  String hostname, int port, final int timeout,
                                                  ConnectionAddressProvider addressProvider,
                                                  MessageTransportFactory transportFactory,
                                                  TCMessageFactory messageFactory) {

    final TCMessageFactory msgFactory;

    if (messageFactory == null) {
      msgFactory = new TCMessageFactoryImpl(sessionProvider, monitor);
      for (Entry<TCMessageType, Class> entry : this.messageTypeClassMapping.entrySet()) {
        msgFactory.addClassMapping(entry.getKey(), entry.getValue());
      }

      for (Entry<TCMessageType, GeneratedMessageFactory> entry : this.messageTypeFactoryMapping.entrySet()) {
        msgFactory.addClassMapping(entry.getKey(), entry.getValue());
      }

    } else {
      msgFactory = messageFactory;
    }

    ClientMessageChannelImpl rv = new ClientMessageChannelImpl(msgFactory, this.messageRouter, sessionProvider,
                                                               new GroupID(addressProvider.getGroupId()),
                                                               addressProvider.getSecurityInfo(), securityManager,
                                                               addressProvider, productId);
    if (transportFactory == null) transportFactory = new MessageTransportFactoryImpl(transportMessageFactory,
                                                                                     connectionHealthChecker,
                                                                                     connectionManager,
                                                                                     addressProvider,
                                                                                     maxReconnectTries, timeout,
                                                                                     callbackPort, handshakeErrHandler,
                                                                                     reconnectionRejectedHandler, securityManager);
    NetworkStackHarness stackHarness = this.stackHarnessFactory.createClientHarness(transportFactory, rv,
                                                                                    new MessageTransportListener[0]);
    stackHarness.finalizeStack();
    return rv;
  }

  /**
   * Creates a network listener with a default network stack.
   */
  @Override
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory) {
    return createListener(sessionProvider, addr, transportDisconnectRemovesChannel, connectionIdFactory, true);
  }

  @Override
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress address,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIDFactory, Sink httpSink) {
    return createListener(sessionProvider, address, transportDisconnectRemovesChannel, connectionIDFactory, true,
                          httpSink, null);
  }

  @Override
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory, boolean reuseAddr) {
    return createListener(sessionProvider, addr, transportDisconnectRemovesChannel, connectionIdFactory, reuseAddr,
                          new NullSink(), null);
  }

  @Override
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory, WireProtocolMessageSink wireProtoMsgSnk) {
    return createListener(sessionProvider, addr, transportDisconnectRemovesChannel, connectionIdFactory, true,
                          new NullSink(), wireProtoMsgSnk);
  }

  /**
   * Creates a network listener with a default network stack.
   */
  private NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                         boolean transportDisconnectRemovesChannel,
                                         ConnectionIDFactory connectionIdFactory, boolean reuseAddr, Sink httpSink,
                                         WireProtocolMessageSink wireProtoMsgSnk) {
    if (shutdown.isSet()) { throw new IllegalStateException("Comms manger shut down"); }

    // The idea here is that someday we might want to pass in a custom channel factory. The reason you might want to do
    // that is so that you can control the actual class of the channels created off this listener
    final TCMessageFactory msgFactory = new TCMessageFactoryImpl(sessionProvider, monitor);

    for (Entry<TCMessageType, Class> entry : this.messageTypeClassMapping.entrySet()) {
      msgFactory.addClassMapping(entry.getKey(), entry.getValue());
    }

    for (Entry<TCMessageType, GeneratedMessageFactory> entry : this.messageTypeFactoryMapping.entrySet()) {
      msgFactory.addClassMapping(entry.getKey(), entry.getValue());
    }

    final ServerMessageChannelFactory channelFactory = new ServerMessageChannelFactory() {
      @Override
      public MessageChannelInternal createNewChannel(ChannelID id, final ProductID productId) {
        return new ServerMessageChannelImpl(id, messageRouter, msgFactory, serverID, productId);
      }
    };

    // XXX: since we don't create multiple listeners per commsMgr, its OK to set
    // L2's callbackPort here. Otherwise, have interface method and set after starting
    // commsMgr listener.
    if (!this.healthCheckerConfig.isCallbackPortListenerNeeded()) {
      this.callbackPort = addr.getPort();
    }

    final ChannelManagerImpl channelManager = new ChannelManagerImpl(transportDisconnectRemovesChannel, channelFactory);
    return new NetworkListenerImpl(addr, this, channelManager, msgFactory, messageRouter, reuseAddr,
                                   connectionIdFactory, httpSink, wireProtoMsgSnk);
  }

  TCListener createCommsListener(TCSocketAddress addr, final ServerMessageChannelFactory channelFactory,
                                 boolean resueAddr, Set initialConnectionIDs, ConnectionIDFactory connectionIdFactory,
                                 Sink httpSink, WireProtocolMessageSink wireProtocolMessageSink) throws IOException {

    MessageTransportFactory transportFactory = new MessageTransportFactory() {
      @Override
      public MessageTransport createNewTransport() {
        throw new AssertionError();
      }

      @Override
      public MessageTransport createNewTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handler,
                                                 TransportHandshakeMessageFactory handshakeMessageFactory,
                                                 List transportListeners) {
        MessageTransport rv = new ServerMessageTransport(connectionID, handler, handshakeMessageFactory);
        rv.addTransportListeners(transportListeners);
        rv.addTransportListener(connectionHealthChecker);
        return rv;
      }

      @Override
      public MessageTransport createNewTransport(ConnectionID connectionId, TCConnection connection,
                                                 TransportHandshakeErrorHandler handler,
                                                 TransportHandshakeMessageFactory handshakeMessageFactory,
                                                 List transportListeners) {
        MessageTransport rv = new ServerMessageTransport(connectionId, connection, handler, handshakeMessageFactory);
        rv.addTransportListeners(transportListeners);
        rv.addTransportListener(connectionHealthChecker);
        return rv;
      }
    };
    ServerStackProvider stackProvider = new ServerStackProvider(initialConnectionIDs, stackHarnessFactory,
                                                                channelFactory, transportFactory,
                                                                this.transportMessageFactory, connectionIdFactory,
                                                                this.connectionPolicy,
                                                                new WireProtocolAdaptorFactoryImpl(httpSink),
                                                                wireProtocolMessageSink, licenseLock, this.commsMgrName,
                                                                this.securityManager);
    return connectionManager.createListener(addr, stackProvider, Constants.DEFAULT_ACCEPT_QUEUE_DEPTH, resueAddr);
  }

  private void startHealthCheckCallbackPortListener(HealthCheckerConfig healthCheckrConfig) {
    if (!healthCheckrConfig.isCallbackPortListenerNeeded()) {
      // Callback Port Listeners are not needed for L2s.
      logger.info("HealthCheck CallbackPort Listener not requested");
      return;
    }

    InetAddress bindAddr;
    String bindAddress = healthCheckrConfig.getCallbackPortListenerBindAddress();
    if (bindAddress == null || bindAddress.equals("")) {
      bindAddress = TCSocketAddress.WILDCARD_IP;
    }

    try {
      bindAddr = InetAddress.getByName(bindAddress);
    } catch (UnknownHostException e) {
      throw new TCRuntimeException("Cannot create InetAddress instance for " + TCSocketAddress.WILDCARD_IP);
    }
    AddressChecker addressChecker = new AddressChecker();
    if (!addressChecker.isLegalBindAddress(bindAddr)) { throw new TCRuntimeException(
                                                                                     "Invalid bind address ["
                                                                                         + bindAddr
                                                                                         + "]. Local addresses are "
                                                                                         + addressChecker
                                                                                             .getAllLocalAddresses()); }

    startCallbackListener(healthCheckrConfig, bindAddr);
  }

  private void startCallbackListener(HealthCheckerConfig healthCheckrConfig, InetAddress bindAddr) {
    for (Integer bindPort : healthCheckrConfig.getCallbackPortListenerBindPort()) {
      if (bindPort == TransportHandshakeMessage.NO_CALLBACK_PORT) {
        logger.info("HealthCheck CallbackPort Listener disabled");
        return;
      }

      TCSocketAddress address = new TCSocketAddress(bindAddr, bindPort);
      NetworkListener callbackPortListener = createListener(new NullSessionManager(), address, true,
                                                            new DefaultConnectionIdFactory());
      try {
        callbackPortListener.start(new HashSet());
        this.callbackPort = callbackPortListener.getBindPort();
        this.callbackportListener = callbackPortListener;
        logger.info("HealthCheck CallbackPort Listener started at " + callbackPortListener.getBindAddress() + ":"
                    + callbackPort);
        return;
      } catch (IOException ioe) {
        if (healthCheckrConfig.getCallbackPortListenerBindPort().size() == 1) {
          logger.warn("Unable to start HealthCheck CallbackPort Listener at" + address + ": " + ioe);
        }
      }
    }

    logger.warn("Unable to start HealthCheck CallbackPort Listener on any port");
  }

  void registerListener(NetworkListener lsnr) {
    synchronized (listeners) {
      boolean added = listeners.add(lsnr);

      if (!added) {
        logger.warn("replaced an existing listener in the listener map");
      }
    }
  }

  void unregisterListener(NetworkListener lsnr) {
    synchronized (listeners) {
      listeners.remove(lsnr);
    }
  }

  /* Following routines are strictly for testing only */
  public ConnectionHealthChecker getConnHealthChecker() {
    return this.connectionHealthChecker;
  }

  public void setConnHealthChecker(ConnectionHealthChecker checker) {
    this.connectionHealthChecker.stop();
    this.connectionHealthChecker = checker;
    this.connectionHealthChecker.start();
  }

  public NetworkListener getCallbackPortListener() {
    return this.callbackportListener;
  }

  public TCMessageRouter getMessageRouter() {
    return this.messageRouter;
  }

}
