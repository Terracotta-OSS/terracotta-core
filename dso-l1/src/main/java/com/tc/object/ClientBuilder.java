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
package com.tc.object;

import com.tc.async.api.Sink;
import com.tc.util.ProductID;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.TCLogger;
import com.tc.management.TCClient;
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

  LongGCLogger createLongGCLogger(long gcTimeOut);

  ClientEntityManager createClientEntityManager(ClientMessageChannel channel);

}
