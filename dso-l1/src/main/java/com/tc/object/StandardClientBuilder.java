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

import org.slf4j.Logger;
import org.terracotta.connection.ConnectionPropertyNames;

import com.tc.async.api.StageManager;
import com.tc.cluster.ClusterInternalEventsGun;
import com.tc.management.TCClient;
import com.tc.net.core.BufferManagerFactory;
import com.tc.net.core.ClearTextBufferManagerFactory;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ClientConnectionErrorListener;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandlerForL1;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.handshakemanager.ClientHandshakeManagerImpl;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.util.ProductID;

import java.util.Map;
import java.util.Properties;


public class StandardClientBuilder implements ClientBuilder {
  
  private final ProductID typeOfClient;
  private volatile ClientConnectionErrorListener listener;

  public StandardClientBuilder(Properties connectionProperties) {
    this.typeOfClient = getTypeOfClient(connectionProperties);
  }

  @Override
  public ClientMessageChannel createClientMessageChannel(CommunicationsManager commMgr,
                                                         SessionProvider sessionProvider, 
                                                         int socketConnectTimeout, TCClient client) {
    ClientMessageChannel cmc = commMgr.createClientChannel(typeOfClient, sessionProvider, socketConnectTimeout);
    if (listener != null){
      cmc.addClientConnectionErrorListener(listener);
    }
    return cmc;
  }

  @Override
  public CommunicationsManager createCommunicationsManager(MessageMonitor monitor, TCMessageRouter messageRouter,
                                                           NetworkStackHarnessFactory stackHarnessFactory,
                                                           ConnectionPolicy connectionPolicy, 
                                                           HealthCheckerConfig aConfig,
                                                           Map<TCMessageType, Class<? extends TCMessage>> messageTypeClassMapping,
                                                           ReconnectionRejectedHandler reconnectionRejectedHandler) {
    return new CommunicationsManagerImpl(CommunicationsManager.COMMSMGR_CLIENT, monitor, messageRouter, stackHarnessFactory, null,
                                         connectionPolicy, 0, aConfig, new TransportHandshakeErrorHandlerForL1(), messageTypeClassMapping,
                                         reconnectionRejectedHandler, getBufferManagerFactory());
  }

  @Override
  public ClientHandshakeManager createClientHandshakeManager(Logger logger,
                                                             ClientHandshakeMessageFactory chmf, 
                                                             SessionManager sessionManager,
                                                             ClusterInternalEventsGun clusterEventsGun, 
                                                             String uuid, 
                                                             String name, 
                                                             String clientVersion,
                                                             ClientEntityManager entity) {
    return new ClientHandshakeManagerImpl(logger, chmf, sessionManager, clusterEventsGun, uuid, name, clientVersion, entity);
  }

  @Override
  public ClientEntityManager createClientEntityManager(ClientMessageChannel channel, StageManager stages) {
    return new ClientEntityManagerImpl(channel, stages);
  }

  protected ProductID getTypeOfClient(Properties connectionProperties) {
    boolean noreconnect =
        Boolean.valueOf(connectionProperties.getProperty(ConnectionPropertyNames.CONNECTION_DISABLE_RECONNECT,
                                                         "false"));
    String typeName = connectionProperties.getProperty(ConnectionPropertyNames.CONNECTION_TYPE);
    ProductID product = (noreconnect) ? ProductID.SERVER : ProductID.PERMANENT;
    try {
      if (typeName != null) {
        product = ProductID.valueOf(typeName);
      }
    } catch (IllegalArgumentException arg) {
      // do nothing, just stick with the default
    }

    return product;
  }

  protected BufferManagerFactory getBufferManagerFactory() {
    return new ClearTextBufferManagerFactory();
  }

  @Override
  public void setClientConnectionErrorListener(ClientConnectionErrorListener listener) {
    this.listener = listener;
  }
}
