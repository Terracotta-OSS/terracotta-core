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
import com.tc.async.api.StageManager;
import com.tc.util.ProductID;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.TCLogger;
import com.tc.management.TCClient;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandlerForL1;
import com.tc.object.config.ConnectionInfoConfig;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.object.context.PauseContext;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.handshakemanager.ClientHandshakeManagerImpl;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.ClientLockManagerConfig;
import com.tc.object.locks.ClientLockManagerImpl;
import com.tc.object.locks.RemoteLockManager;
import com.tc.object.locks.RemoteLockManagerImpl;
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

public class StandardClientBuilder implements ClientBuilder {

  @Override
  public ClientMessageChannel createClientMessageChannel(CommunicationsManager commMgr,
                                                         PreparedComponentsFromL2Connection connComp,
                                                         SessionProvider sessionProvider, int maxReconnectTries,
                                                         int socketConnectTimeout, TCClient client) {
    final ConnectionAddressProvider cap = createConnectionAddressProvider(connComp);
    return commMgr.createClientChannel(sessionProvider, maxReconnectTries, null, 0, socketConnectTimeout, cap);
  }

  protected ConnectionAddressProvider createConnectionAddressProvider(PreparedComponentsFromL2Connection connComp) {
    final ConnectionInfoConfig connectionInfoItem = connComp.createConnectionInfoConfigItem();
    final ConnectionInfo[] connectionInfo = connectionInfoItem.getConnectionInfos();
    final ConnectionAddressProvider cap = new ConnectionAddressProvider(connectionInfo);
    return cap;
  }

  @Override
  public CommunicationsManager createCommunicationsManager(MessageMonitor monitor, TCMessageRouter messageRouter,
                                                           NetworkStackHarnessFactory stackHarnessFactory,
                                                           ConnectionPolicy connectionPolicy, int commThread,
                                                           HealthCheckerConfig aConfig,
                                                           Map<TCMessageType, Class<? extends TCMessage>> messageTypeClassMapping,
                                                           ReconnectionRejectedHandler reconnectionRejectedHandler,
                                                           TCSecurityManager securityManager, ProductID productId) {
    return new CommunicationsManagerImpl(CommunicationsManager.COMMSMGR_CLIENT, monitor, messageRouter, stackHarnessFactory, null,
                                         connectionPolicy, 0, aConfig, new TransportHandshakeErrorHandlerForL1(), messageTypeClassMapping,
                                         reconnectionRejectedHandler, securityManager, productId);
  }

  @Override
  public ClientLockManager createLockManager(ClientMessageChannel channel, ClientIDLogger clientIDLogger,
                                             SessionManager sessionManager,
                                             LockRequestMessageFactory lockRequestMessageFactory,
                                             ThreadIDManager threadManager,
                                             ClientLockManagerConfig config,
                                             TaskRunner taskRunner) {
    final RemoteLockManager remoteLockManager = new RemoteLockManagerImpl(channel, lockRequestMessageFactory,
        taskRunner);
    return new ClientLockManagerImpl(clientIDLogger, sessionManager, channel, remoteLockManager, threadManager, config,
        taskRunner);
  }

  @Override
  public ClientHandshakeManager createClientHandshakeManager(TCLogger logger,
                                                             ClientHandshakeMessageFactory chmf, Sink<PauseContext> pauseSink,
                                                             SessionManager sessionManager,
                                                             ClusterInternalEventsGun clusterEventsGun, String clientVersion,
                                                             Collection<ClientHandshakeCallback> callbacks) {
    return new ClientHandshakeManagerImpl(logger, chmf, sessionManager, clusterEventsGun, clientVersion, callbacks);
  }

  @Override
  public LongGCLogger createLongGCLogger(long gcTimeOut) {
    return new LongGCLogger(gcTimeOut);
  }

  @Override
  public ClientEntityManager createClientEntityManager(ClientMessageChannel channel, StageManager stages) {
    return new ClientEntityManagerImpl(channel, stages);
  }

}
