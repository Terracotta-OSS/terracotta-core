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

import com.tc.net.protocol.transport.ClientConnectionErrorListener;
import org.slf4j.Logger;

import com.tc.net.core.ProductID;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.net.core.TCConnectionManager;

import java.util.Map;
import com.tc.net.protocol.tcm.TCAction;


public interface ClientBuilder {
  ClientMessageChannel createClientMessageChannel(CommunicationsManager commMgr,
                                                     int socketConnectTimeout);

  CommunicationsManager createCommunicationsManager(MessageMonitor monitor,
                                                    TCMessageRouter messageRouter,
                                                    NetworkStackHarnessFactory stackHarnessFactory,
                                                    ConnectionPolicy connectionPolicy,
                                                    TCConnectionManager connections,
                                                    HealthCheckerConfig hcConfig,
                                                    Map<TCMessageType, Class<? extends TCAction>> messageTypeClassMapping,
                                                    ReconnectionRejectedHandler reconnectionRejectedBehaviour);

  ClientHandshakeManager createClientHandshakeManager(Logger logger,
                                                      ClientHandshakeMessageFactory chmf,
                                                      String uuid,
                                                      String name,
                                                      String clientVersion,
                                                      String clientRevision,
                                                      ClientEntityManager entity);

  ClientEntityManager createClientEntityManager(ClientMessageChannel channel);

  void setClientConnectionErrorListener(ClientConnectionErrorListener listener);

  ProductID getTypeOfClient();

  TCConnectionManager createConnectionManager(String uuid, String name);
}
