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

import com.tc.net.core.TCConnectionManagerImpl;
import org.slf4j.Logger;
import org.terracotta.connection.ConnectionPropertyNames;

import com.tc.net.core.SocketEndpointFactory;
import com.tc.net.core.ProductID;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
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

import java.util.Map;
import java.util.Properties;
import com.tc.net.protocol.tcm.TCAction;


public class StandardClientBuilder implements ClientBuilder {
  
  private final Properties connectionProperties;
  private volatile ClientConnectionErrorListener listener;
  private final SocketEndpointFactory buffers;

  public StandardClientBuilder(Properties connectionProperties, SocketEndpointFactory buffers) {
    this.connectionProperties = connectionProperties;
    this.buffers = buffers;
  }

  @Override
  public ClientMessageChannel createClientMessageChannel(CommunicationsManager commMgr,
                                                         int socketConnectTimeout) {
    ClientMessageChannel cmc = commMgr.createClientChannel(getTypeOfClient(), socketConnectTimeout);
    if (listener != null){
      cmc.addClientConnectionErrorListener(listener);
    }
    return cmc;
  }

  @Override
  public CommunicationsManager createCommunicationsManager(MessageMonitor monitor, TCMessageRouter messageRouter,
                                                           NetworkStackHarnessFactory stackHarnessFactory,
                                                           ConnectionPolicy connectionPolicy, 
                                                           TCConnectionManager connections,
                                                           HealthCheckerConfig aConfig,
                                                           Map<TCMessageType, Class<? extends TCAction>> messageTypeClassMapping,
                                                           ReconnectionRejectedHandler reconnectionRejectedHandler) {
    return new CommunicationsManagerImpl(monitor, messageRouter, stackHarnessFactory, connections,
                                         connectionPolicy, aConfig, new TransportHandshakeErrorHandlerForL1(), messageTypeClassMapping,
                                         reconnectionRejectedHandler, getSocketEndpointFactory());
  }

  @Override
  public ClientHandshakeManager createClientHandshakeManager(Logger logger,
                                                             ClientHandshakeMessageFactory chmf, 
                                                             String uuid, 
                                                             String name, 
                                                             String clientVersion,
                                                             String clientRevision,
                                                             ClientEntityManager entity) {
    return new ClientHandshakeManagerImpl(logger, chmf, uuid, name, clientVersion, clientRevision, entity);
  }

  @Override
  public ClientEntityManager createClientEntityManager(ClientMessageChannel channel) {
    return new ClientEntityManagerImpl(channel);
  }

  @Override
  public ProductID getTypeOfClient() {
    boolean noreconnect =
        Boolean.valueOf(connectionProperties.getProperty(ConnectionPropertyNames.CONNECTION_DISABLE_RECONNECT,
                                                         "false"));
    String typeName = connectionProperties.getProperty(ConnectionPropertyNames.CONNECTION_TYPE);
    ProductID product = (noreconnect) ? ProductID.SERVER : ProductID.PERMANENT;
    try {
      if (typeName != null) {
        product = ProductID.valueOf(typeName.toUpperCase());
      }
    } catch (IllegalArgumentException arg) {
      // do nothing, just stick with the default
    }

    return product;
  }

  @Override
  public TCConnectionManager createConnectionManager(String uuid, String name) {
    return new TCConnectionManagerImpl(name + "/" + uuid, 0, getSocketEndpointFactory());
  }

  protected SocketEndpointFactory getSocketEndpointFactory() {
    return buffers;
  }

  @Override
  public void setClientConnectionErrorListener(ClientConnectionErrorListener listener) {
    this.listener = listener;
  }
}
