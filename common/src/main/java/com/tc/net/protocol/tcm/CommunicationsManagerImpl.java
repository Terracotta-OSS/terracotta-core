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
package com.tc.net.protocol.tcm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.core.ClearTextBufferManagerFactory;
import com.tc.net.ServerID;
import com.tc.net.core.BufferManagerFactory;
import com.tc.net.core.Constants;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCListener;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionHealthChecker;
import com.tc.net.protocol.transport.ConnectionHealthCheckerEchoImpl;
import com.tc.net.protocol.transport.ConnectionHealthCheckerImpl;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionPolicy;
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
import com.tc.net.protocol.transport.TransportHandshakeMessageFactory;
import com.tc.net.protocol.transport.TransportMessageFactoryImpl;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactoryImpl;
import com.tc.net.protocol.transport.WireProtocolMessageSink;
import com.tc.net.core.ProductID;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Communications manager for setting up listeners and creating client connections
 * 
 * @author teck
 */
public class CommunicationsManagerImpl implements CommunicationsManager {
  private static final Logger logger = LoggerFactory.getLogger(CommunicationsManager.class);

  private final SetOnceFlag                                                    shutdown                  = new SetOnceFlag();
  private final Set<NetworkListener>                                           listeners                 = new HashSet<NetworkListener>();
  private final ReentrantLock                                                  licenseLock               = new ReentrantLock();
  private final TCConnectionManager                                            connectionManager;
  private final NetworkStackHarnessFactory                                     stackHarnessFactory;
  private final TransportHandshakeMessageFactory                               transportMessageFactory;
  private final MessageMonitor                                                 monitor;
  private final TCMessageRouter                                                messageRouter;
  private final HealthCheckerConfig                                            healthCheckerConfig;
  private final ConnectionPolicy                                               connectionPolicy;
  private final ReconnectionRejectedHandler                                    reconnectionRejectedHandler;
  protected final ConcurrentHashMap<TCMessageType, Class<? extends TCAction>> messageTypeClassMapping   = new ConcurrentHashMap<TCMessageType, Class<? extends TCAction>>();
  protected final ConcurrentHashMap<TCMessageType, GeneratedMessageFactory>    messageTypeFactoryMapping = new ConcurrentHashMap<TCMessageType, GeneratedMessageFactory>();

  private final ConnectionHealthChecker                                              connectionHealthChecker;
  private ServerID                                                             serverID                  = ServerID.NULL_ID;
  private final TransportHandshakeErrorHandler                                 handshakeErrHandler;

  /**
   * Create a communications manager. This implies that one or more network handling threads will be started on your
   * behalf. As such, you should not be instantiating one of these per connection for instance.
   */
  public CommunicationsManagerImpl(MessageMonitor monitor,
                                   NetworkStackHarnessFactory stackHarnessFactory, TCConnectionManager connectionManager, ConnectionPolicy connectionPolicy) {
    this(monitor, new TCMessageRouterImpl(), stackHarnessFactory, connectionManager, connectionPolicy,
         new DisabledHealthCheckerConfigImpl(), new TransportHandshakeErrorHandlerForL1(), Collections.<TCMessageType, Class<? extends TCAction>>emptyMap(),
         Collections.<TCMessageType, GeneratedMessageFactory>emptyMap());
  }

  public CommunicationsManagerImpl(MessageMonitor monitor, TCMessageRouter messageRouter,
                                   NetworkStackHarnessFactory stackHarnessFactory, TCConnectionManager connectionManager, ConnectionPolicy connectionPolicy,
                                   HealthCheckerConfig config, ServerID serverID,
                                   TransportHandshakeErrorHandler transportHandshakeErrorHandler,
                                   Map<TCMessageType, Class<? extends TCAction>> messageTypeClassMapping,
                                   Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping) {
    this(monitor, messageRouter, stackHarnessFactory, connectionManager, connectionPolicy, config,
         transportHandshakeErrorHandler, messageTypeClassMapping, ReconnectionRejectedHandlerL2.SINGLETON, new ClearTextBufferManagerFactory());
    this.serverID = serverID;
  }

  public CommunicationsManagerImpl(MessageMonitor monitor, TCMessageRouter messageRouter,
                                   NetworkStackHarnessFactory stackHarnessFactory, TCConnectionManager connectionManager,
                                   ConnectionPolicy connectionPolicy,
                                   HealthCheckerConfig config, ServerID serverID,
                                   TransportHandshakeErrorHandler transportHandshakeErrorHandler,
                                   Map<TCMessageType, Class<? extends TCAction>> messageTypeClassMapping,
                                   Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping,
                                   BufferManagerFactory bufferManagerFactory) {
    this(monitor, messageRouter, stackHarnessFactory, connectionManager, connectionPolicy, config,
         transportHandshakeErrorHandler, messageTypeClassMapping, ReconnectionRejectedHandlerL2.SINGLETON, bufferManagerFactory);
    this.serverID = serverID;
  }

  public CommunicationsManagerImpl(MessageMonitor monitor, TCMessageRouter messageRouter,
                                   NetworkStackHarnessFactory stackHarnessFactory, TCConnectionManager connMgr,
                                   ConnectionPolicy connectionPolicy,
                                   HealthCheckerConfig healthCheckerConf,
                                   TransportHandshakeErrorHandler transportHandshakeErrorHandler,
                                   Map<TCMessageType, Class<? extends TCAction>> messageTypeClassMapping,
                                   Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping) {
    this(monitor, messageRouter, stackHarnessFactory, connMgr, connectionPolicy,
         healthCheckerConf, transportHandshakeErrorHandler, messageTypeClassMapping,
         ReconnectionRejectedHandlerL1.SINGLETON, new ClearTextBufferManagerFactory());
  }

  /**
   * Create a comms manager with the given connection manager. This cstr is mostly for testing, or in the event that you
   * actually want to use an explicit connection manager
   * 
   * @param connMgr the connection manager to use
   */
  public CommunicationsManagerImpl(MessageMonitor monitor, TCMessageRouter messageRouter,
                                   NetworkStackHarnessFactory stackHarnessFactory, TCConnectionManager connMgr,
                                   ConnectionPolicy connectionPolicy, 
                                   HealthCheckerConfig healthCheckerConf,
                                   TransportHandshakeErrorHandler transportHandshakeErrorHandler,
                                   Map<TCMessageType, Class<? extends TCAction>> messageTypeClassMapping,
                                   ReconnectionRejectedHandler reconnectionRejectedHandler,
                                   BufferManagerFactory bufferManagerFactory) {
    this.monitor = monitor;
    this.messageRouter = messageRouter;
    this.transportMessageFactory = new TransportMessageFactoryImpl();
    this.connectionPolicy = connectionPolicy;
    this.stackHarnessFactory = stackHarnessFactory;
    this.healthCheckerConfig = healthCheckerConf;
    this.handshakeErrHandler = transportHandshakeErrorHandler;
    this.messageTypeClassMapping.putAll(messageTypeClassMapping);
    this.messageTypeFactoryMapping.putAll(messageTypeFactoryMapping);
    this.reconnectionRejectedHandler = reconnectionRejectedHandler;

    Assert.assertNotNull(connMgr);
    this.connectionManager = connMgr;

    Assert.eval(healthCheckerConfig != null);
    connectionHealthChecker = healthCheckerConfig.isHealthCheckerEnabled() ?
            new ConnectionHealthCheckerImpl(healthCheckerConfig, connectionManager, createReferenceCheck()) :
            new ConnectionHealthCheckerEchoImpl();
  }
  
  private Supplier<Boolean> createReferenceCheck() {
// reference this manager.  If it not reachable, all connections and threads associated need to be cleaned up.
    ReferenceQueue<Object> gc = new ReferenceQueue<>();
    PhantomReference<Object> ref = new PhantomReference<>(this, gc);
    SetOnceFlag gcd = new SetOnceFlag();
    return ()-> {
      if (gc.poll() == ref) {
        gcd.set();
      }
      return !gcd.isSet();
    };
  }
  
  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("connectionPolicy", this.connectionPolicy.toString());
    map.put("connectionManager", this.connectionManager.getStateMap());
    return map;
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
      NetworkListener[] col = getAllListeners();
      for (NetworkListener l : col) {
        try {
          l.stop(0);
        } catch (TCTimeoutException to) {
          logger.info("failed to shutdown listener", to);
        }
      }
      connectionManager.shutdown();
      monitor.shutdown();
    } else {
      logger.warn("shutdown already started");
    }
  }
  
  @Override
  public NetworkListener[] getAllListeners() {
    synchronized (listeners) {
      return listeners.toArray(new NetworkListener[listeners.size()]);
    }
  }

  @Override
  public void addClassMapping(TCMessageType messageType, Class<? extends TCAction> messageClass) {
    messageTypeClassMapping.put(messageType, messageClass);
  }

  @Override
  public ClientMessageChannel createClientChannel(ProductID productId, int timeout) {
    return createClientChannel(productId, timeout, null, null);
  }
  
  public ClientMessageChannel createClientChannel(ProductID productId, 
                                                  int timeout, 
                                                  MessageTransportFactory transportFactory,
                                                  TCMessageFactory messageFactory) {

    final TCMessageFactory msgFactory;

    if (messageFactory == null) {
      msgFactory = new TCMessageFactoryImpl(monitor);
      for (Entry<TCMessageType, Class<? extends TCAction>> entry : this.messageTypeClassMapping.entrySet()) {
        msgFactory.addClassMapping(entry.getKey(), entry.getValue());
      }

      for (Entry<TCMessageType, GeneratedMessageFactory> entry : this.messageTypeFactoryMapping.entrySet()) {
        msgFactory.addClassMapping(entry.getKey(), entry.getValue());
      }

    } else {
      msgFactory = messageFactory;
    }

    ClientMessageChannelImpl rv = new ClientMessageChannelImpl(msgFactory, this.messageRouter, productId);
    if (transportFactory == null) transportFactory = new MessageTransportFactoryImpl(transportMessageFactory,
                                                                                     connectionHealthChecker,
                                                                                     connectionManager,
                                                                                     timeout,
                                                                                     handshakeErrHandler,
                                                                                     reconnectionRejectedHandler
    );
    NetworkStackHarness stackHarness = this.stackHarnessFactory.createClientHarness(transportFactory, rv,
                                                                                    new MessageTransportListener[0]);
    stackHarness.finalizeStack();
    return rv;
  }

  /**
   * Creates a network listener with a default network stack.
   */
  @Override
  public NetworkListener createListener(InetSocketAddress addr, boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory, RedirectAddressProvider activeNameProvider) {
    return createListener(addr, (c)->transportDisconnectRemovesChannel, connectionIdFactory, true, null, activeNameProvider, (t)->true);
  }

  @Override
  public NetworkListener createListener(InetSocketAddress addr, Predicate<MessageChannel> transportDisconnectRemovesChannel,
          ConnectionIDFactory connectionIdFactory, Predicate<MessageTransport> validation) {
    return createListener(addr, transportDisconnectRemovesChannel, connectionIdFactory, true, null, null, validation);
  }

  /**
   * Creates a network listener with a default network stack.
   */
  NetworkListener createListener(InetSocketAddress addr,
                                         Predicate<MessageChannel> transportDisconnectRemovesChannel,
                                         ConnectionIDFactory connectionIdFactory, boolean reuseAddr,
                                         WireProtocolMessageSink wireProtoMsgSnk, RedirectAddressProvider activeProvider, Predicate<MessageTransport> validation) {
    if (shutdown.isSet()) { throw new IllegalStateException("Comms manger shut down"); }

    // The idea here is that someday we might want to pass in a custom channel factory. The reason you might want to do
    // that is so that you can control the actual class of the channels created off this listener
    final TCMessageFactory msgFactory = new TCMessageFactoryImpl(monitor);

    for (Entry<TCMessageType, Class<? extends TCAction>> entry : this.messageTypeClassMapping.entrySet()) {
      msgFactory.addClassMapping(entry.getKey(), entry.getValue());
    }

    for (Entry<TCMessageType, GeneratedMessageFactory> entry : this.messageTypeFactoryMapping.entrySet()) {
      msgFactory.addClassMapping(entry.getKey(), entry.getValue());
    }

    final ServerMessageChannelFactory channelFactory = new ServerMessageChannelFactory() {
      @Override
      public MessageChannelInternal createNewChannel(ChannelID id) {
        return new ServerMessageChannelImpl(id, messageRouter, msgFactory, serverID);
      }
    };

    final ChannelManagerImpl channelManager = new ChannelManagerImpl(transportDisconnectRemovesChannel, channelFactory);
    return new NetworkListenerImpl(addr, this, channelManager, msgFactory, reuseAddr,
                                   connectionIdFactory, wireProtoMsgSnk, activeProvider, validation);
  }

  TCListener createCommsListener(InetSocketAddress addr, ServerMessageChannelFactory channelFactory,
                                 boolean resueAddr, Set<ConnectionID> initialConnectionIDs, RedirectAddressProvider activeProvider, Predicate<MessageTransport> validation, ConnectionIDFactory connectionIdFactory,
                                 WireProtocolMessageSink wireProtocolMessageSink) throws IOException {

    MessageTransportFactory transportFactory = new MessageTransportFactory() {      
      @Override
      public ClientMessageTransport createNewTransport() {
        throw new AssertionError();
      }

      @Override
      public ServerMessageTransport createNewTransport(TransportHandshakeErrorHandler handler,
                                                 TransportHandshakeMessageFactory handshakeMessageFactory,
                                                 List<MessageTransportListener> transportListeners) {
        ServerMessageTransport rv = new ServerMessageTransport(handler, handshakeMessageFactory);
        rv.addTransportListeners(transportListeners);
        rv.addTransportListener(connectionHealthChecker);
        return rv;
      }

      @Override
      public ServerMessageTransport createNewTransport(TCConnection connection,
                                                 TransportHandshakeErrorHandler handler,
                                                 TransportHandshakeMessageFactory handshakeMessageFactory,
                                                 List<MessageTransportListener> transportListeners) {
        ServerMessageTransport rv = new ServerMessageTransport(connection, handler, handshakeMessageFactory);
        rv.addTransportListeners(transportListeners);
        rv.addTransportListener(connectionHealthChecker);
        return rv;
      }
    };
    ServerStackProvider stackProvider = new ServerStackProvider(initialConnectionIDs, activeProvider, validation, stackHarnessFactory,
                                                                channelFactory, transportFactory,
                                                                this.transportMessageFactory, connectionIdFactory,
                                                                this.connectionPolicy,
                                                                new WireProtocolAdaptorFactoryImpl(),
                                                                wireProtocolMessageSink, licenseLock);
    return connectionManager.createListener(addr, stackProvider, Constants.DEFAULT_ACCEPT_QUEUE_DEPTH, resueAddr);
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

  public TCMessageRouter getMessageRouter() {
    return this.messageRouter;
  }

}
