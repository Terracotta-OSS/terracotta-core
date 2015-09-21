/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.Sink;
import com.tc.license.ProductID;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.TCLogger;
import com.tc.management.L1Management;
import com.tc.management.TCClient;
import com.tc.management.remote.protocol.terracotta.TunneledDomainManager;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.object.config.DSOMBeanConfig;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.object.context.PauseContext;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.ClientLockManagerConfig;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.util.UUID;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.runtime.ThreadIDManager;
import com.tcclient.cluster.ClusterInternalEventsGun;

import java.util.Collection;
import java.util.Map;

public interface ClientBuilder {

  ClientMessageChannel createClientMessageChannel(CommunicationsManager commMgr,
                                                     PreparedComponentsFromL2Connection connComp,
                                                     SessionProvider sessionProvider, int maxReconnectTries,
                                                     int socketConnectTimeout, TCClient client);

  CommunicationsManager createCommunicationsManager(MessageMonitor monitor,
                                                    TCMessageRouter messageRouter,
                                                    NetworkStackHarnessFactory stackHarnessFactory,
                                                    ConnectionPolicy connectionPolicy,
                                                    int workerCommThreads,
                                                    HealthCheckerConfig hcConfig,
                                                    Map<TCMessageType, Class<? extends TCMessage>> messageTypeClassMapping,
                                                    ReconnectionRejectedHandler reconnectionRejectedBehaviour,
                                                    TCSecurityManager securityManager, ProductID productId);

  TunnelingEventHandler createTunnelingEventHandler(ClientMessageChannel ch, DSOMBeanConfig config, UUID uuid);

  TunneledDomainManager createTunneledDomainManager(ClientMessageChannel ch, DSOMBeanConfig config,
                                                    TunnelingEventHandler teh);

  ClientLockManager createLockManager(ClientMessageChannel dsoChannel, ClientIDLogger clientIDLogger,
                                      SessionManager sessionManager,
                                      LockRequestMessageFactory lockRequestMessageFactory,
                                      ThreadIDManager threadManager,
                                      ClientLockManagerConfig clientLockManagerConfig,
                                      TaskRunner taskRunner);

  ClientHandshakeManager createClientHandshakeManager(TCLogger logger,
                                                      ClientHandshakeMessageFactory chmf, Sink<PauseContext> pauseSink,
                                                      SessionManager sessionManager,
                                                      ClusterInternalEventsGun clusterEventsGun,
                                                      String clientVersion,
                                                      Collection<ClientHandshakeCallback> callbacks);

  L1Management createL1Management(TunnelingEventHandler teh, String rawConfigText,
                                  DistributedObjectClient distributedObjectClient);

  void registerForOperatorEvents(L1Management management);

  LongGCLogger createLongGCLogger(long gcTimeOut);

  ClientEntityManager createClientEntityManager(ClientMessageChannel channel);

}
