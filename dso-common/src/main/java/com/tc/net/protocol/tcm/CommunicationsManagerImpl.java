/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.exception.TCRuntimeException;
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
import com.tc.util.concurrent.ConcurrentHashMap;
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
  protected final ConcurrentHashMap<TCMessageType, Class>                   messageTypeClassMapping   = new ConcurrentHashMap<TCMessageType, Class>();
  protected final ConcurrentHashMap<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping = new ConcurrentHashMap<TCMessageType, GeneratedMessageFactory>();

  private ConnectionHealthChecker                                           connectionHealthChecker;
  private ServerID                                                          serverID                  = ServerID.NULL_ID;
  private int                                                               callbackPort              = TransportHandshakeMessage.NO_CALLBACK_PORT;
  private NetworkListener                                                   callbackportListener      = null;
  private final TransportHandshakeErrorHandler                              handshakeErrHandler;
  private final String                                                      commsMgrName;

  /**
   * Create a communications manager. This implies that one or more network handling threads will be started on your
   * behalf. As such, you should not be instantiating one of these per connection for instance.
   */
  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor,
                                   NetworkStackHarnessFactory stackHarnessFactory, ConnectionPolicy connectionPolicy) {
    this(commsMgrName, monitor, new TCMessageRouterImpl(), stackHarnessFactory, null, connectionPolicy, 0,
         new DisabledHealthCheckerConfigImpl(), new TransportHandshakeErrorHandlerForL1(), Collections.EMPTY_MAP,
         Collections.EMPTY_MAP);

  }

  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor,
                                   NetworkStackHarnessFactory stackHarnessFactory, ConnectionPolicy connectionPolicy,
                                   int workerCommCount) {
    this(commsMgrName, monitor, new TCMessageRouterImpl(), stackHarnessFactory, null, connectionPolicy,
         workerCommCount, new DisabledHealthCheckerConfigImpl(), new TransportHandshakeErrorHandlerForL1(),
         Collections.EMPTY_MAP, Collections.EMPTY_MAP);
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
         new TransportHandshakeErrorHandlerForL1(), messageTypeClassMapping, messageTypeFactoryMapping);
  }

  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor, TCMessageRouter messageRouter,
                                   NetworkStackHarnessFactory stackHarnessFactory, ConnectionPolicy connectionPolicy,
                                   int workerCommCount, HealthCheckerConfig config, ServerID serverID,
                                   TransportHandshakeErrorHandler transportHandshakeErrorHandler,
                                   Map<TCMessageType, Class> messageTypeClassMapping,
                                   Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping) {
    this(commsMgrName, monitor, messageRouter, stackHarnessFactory, null, connectionPolicy, workerCommCount, config,
         transportHandshakeErrorHandler, messageTypeClassMapping, messageTypeFactoryMapping);
    this.serverID = serverID;
  }

  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor, TCMessageRouter messageRouter,
                                   NetworkStackHarnessFactory stackHarnessFactory, TCConnectionManager connMgr,
                                   ConnectionPolicy connectionPolicy, int workerCommCount,
                                   HealthCheckerConfig healthCheckerConf,
                                   TransportHandshakeErrorHandler transportHandshakeErrorHandler,
                                   Map<TCMessageType, Class> messageTypeClassMapping,
                                   Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping) {
    this(commsMgrName, monitor, messageRouter, stackHarnessFactory, connMgr, connectionPolicy, workerCommCount,
         healthCheckerConf, transportHandshakeErrorHandler, messageTypeClassMapping, messageTypeFactoryMapping,
         ReconnectionRejectedHandler.DEFAULT_BEHAVIOUR);
  }

  /**
   * Create a comms manager with the given connection manager. This cstr is mostly for testing, or in the event that you
   * actually want to use an explicit connection manager
   * 
   * @param connMgr the connection manager to use
   * @param serverDescriptors
   */
  public CommunicationsManagerImpl(String commsMgrName, MessageMonitor monitor, TCMessageRouter messageRouter,
                                   NetworkStackHarnessFactory stackHarnessFactory, TCConnectionManager connMgr,
                                   ConnectionPolicy connectionPolicy, int workerCommCount,
                                   HealthCheckerConfig healthCheckerConf,
                                   TransportHandshakeErrorHandler transportHandshakeErrorHandler,
                                   Map<TCMessageType, Class> messageTypeClassMapping,
                                   Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping,
                                   ReconnectionRejectedHandler reconnectionRejectedHandler) {
    this.commsMgrName = commsMgrName;
    this.monitor = monitor;
    this.messageRouter = messageRouter;
    this.transportMessageFactory = new TransportMessageFactoryImpl();
    this.connectionPolicy = connectionPolicy;
    this.stackHarnessFactory = stackHarnessFactory;
    this.healthCheckerConfig = healthCheckerConf;
    this.handshakeErrHandler = transportHandshakeErrorHandler;
    this.privateConnMgr = (connMgr == null);
    this.messageTypeClassMapping.putAll(messageTypeClassMapping);
    this.messageTypeFactoryMapping.putAll(messageTypeFactoryMapping);
    this.reconnectionRejectedHandler = reconnectionRejectedHandler;

    Assert.assertNotNull(commsMgrName);
    if (null == connMgr) {
      this.connectionManager = new TCConnectionManagerImpl(commsMgrName, workerCommCount, healthCheckerConfig);
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

  public TCConnectionManager getConnectionManager() {
    return this.connectionManager;
  }

  public boolean isInShutdown() {
    return shutdown.isSet();
  }

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

  public NetworkListener[] getAllListeners() {
    synchronized (listeners) {
      return (NetworkListener[]) listeners.toArray(new NetworkListener[listeners.size()]);
    }
  }

  public void addClassMapping(TCMessageType messageType, Class messageClass) {
    messageTypeClassMapping.put(messageType, messageClass);
  }

  public void addClassMapping(TCMessageType messageType, GeneratedMessageFactory generatedMessageFactory) {
    messageTypeFactoryMapping.put(messageType, generatedMessageFactory);
  }

  public ClientMessageChannel createClientChannel(final SessionProvider sessionProvider, final int maxReconnectTries,
                                                  String hostname, int port, final int timeout,
                                                  ConnectionAddressProvider addressProvider) {
    return createClientChannel(sessionProvider, maxReconnectTries, hostname, port, timeout, addressProvider, null, null);

  }

  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, final int maxReconnectTries,
                                                  String hostname, int port, final int timeout,
                                                  ConnectionAddressProvider addressProvider,
                                                  MessageTransportFactory transportFactory) {
    return createClientChannel(sessionProvider, maxReconnectTries, hostname, port, timeout, addressProvider,
                               transportFactory, null);
  }

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
                                                               new GroupID(addressProvider.getGroupId()));
    if (transportFactory == null) transportFactory = new MessageTransportFactoryImpl(transportMessageFactory,
                                                                                     connectionHealthChecker,
                                                                                     connectionManager,
                                                                                     addressProvider,
                                                                                     maxReconnectTries, timeout,
                                                                                     callbackPort, handshakeErrHandler,
                                                                                     reconnectionRejectedHandler);
    NetworkStackHarness stackHarness = this.stackHarnessFactory.createClientHarness(transportFactory, rv,
                                                                                    new MessageTransportListener[0]);
    stackHarness.finalizeStack();
    return rv;
  }

  /**
   * Creates a network listener with a default network stack.
   */
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory) {
    return createListener(sessionProvider, addr, transportDisconnectRemovesChannel, connectionIdFactory, true);
  }

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress address,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIDFactory, Sink httpSink) {
    return createListener(sessionProvider, address, transportDisconnectRemovesChannel, connectionIDFactory, true,
                          httpSink, null);
  }

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory, boolean reuseAddr) {
    return createListener(sessionProvider, addr, transportDisconnectRemovesChannel, connectionIdFactory, reuseAddr,
                          new NullSink(), null);
  }

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
      public MessageChannelInternal createNewChannel(ChannelID id) {
        return new ServerMessageChannelImpl(id, messageRouter, msgFactory, serverID);
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
      public MessageTransport createNewTransport() {
        throw new AssertionError();
      }

      public MessageTransport createNewTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handler,
                                                 TransportHandshakeMessageFactory handshakeMessageFactory,
                                                 List transportListeners) {
        MessageTransport rv = new ServerMessageTransport(connectionID, handler, handshakeMessageFactory);
        rv.addTransportListeners(transportListeners);
        rv.addTransportListener(connectionHealthChecker);
        return rv;
      }

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
    ServerStackProvider stackProvider = new ServerStackProvider(TCLogging.getLogger(ServerStackProvider.class),
                                                                initialConnectionIDs, stackHarnessFactory,
                                                                channelFactory, transportFactory,
                                                                this.transportMessageFactory, connectionIdFactory,
                                                                this.connectionPolicy,
                                                                new WireProtocolAdaptorFactoryImpl(httpSink),
                                                                wireProtocolMessageSink, licenseLock, this.commsMgrName);
    return connectionManager.createListener(addr, stackProvider, Constants.DEFAULT_ACCEPT_QUEUE_DEPTH, resueAddr);
  }

  private void startHealthCheckCallbackPortListener(HealthCheckerConfig healthCheckrConfig) {
    if (!healthCheckrConfig.isCallbackPortListenerNeeded()) {
      // Callback Port Listeners are not needed for L2s.
      logger.info("HealtCheck CallbackPort Listener not requested");
      return;
    }

    int bindPort = healthCheckrConfig.getCallbackPortListenerBindPort();
    if (bindPort == TransportHandshakeMessage.NO_CALLBACK_PORT) {
      logger.info("HealtCheck CallbackPort Listener disabled");
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

    TCSocketAddress address = new TCSocketAddress(bindAddr, bindPort);
    NetworkListener callbackPortListener = createListener(new NullSessionManager(), address, true,
                                                          new DefaultConnectionIdFactory());
    try {
      callbackPortListener.start(new HashSet());
      this.callbackPort = callbackPortListener.getBindPort();
      this.callbackportListener = callbackPortListener;
      logger.info("HealthCheck CallbackPort Listener started at " + callbackPortListener.getBindAddress() + ":"
                  + callbackPort);
    } catch (IOException ioe) {
      logger.info("Unable to start HealthCheck CallbackPort Listener at" + address + ": " + ioe);
    }
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
