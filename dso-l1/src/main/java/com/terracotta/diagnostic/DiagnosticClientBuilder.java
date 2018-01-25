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
package com.terracotta.diagnostic;


import com.tc.async.api.StageManager;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.DisabledHealthCheckerConfigImpl;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.object.ClientEntityManager;
import com.tc.object.StandardClientBuilder;
import com.tc.util.ProductID;
import java.util.Map;

import java.util.Properties;


public class DiagnosticClientBuilder extends StandardClientBuilder {

  public DiagnosticClientBuilder(Properties connectionProperties) {
    super(connectionProperties);
  }

  @Override
  public CommunicationsManager createCommunicationsManager(MessageMonitor monitor, TCMessageRouter messageRouter, NetworkStackHarnessFactory stackHarnessFactory, ConnectionPolicy connectionPolicy, HealthCheckerConfig aConfig, Map<TCMessageType, Class<? extends TCMessage>> messageTypeClassMapping, ReconnectionRejectedHandler reconnectionRejectedHandler) {
    return super.createCommunicationsManager(monitor, messageRouter, stackHarnessFactory, connectionPolicy, new DisabledHealthCheckerConfigImpl(), messageTypeClassMapping, reconnectionRejectedHandler); 
  }
  
  @Override
  public ClientEntityManager createClientEntityManager(ClientMessageChannel channel, StageManager stages) {
    return new DiagnosticClientEntityManager(channel);
  }

  @Override
  protected ProductID getTypeOfClient(Properties connectionProperties) {
    return ProductID.DIAGNOSTIC;
  }
}
