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
package com.tc.object.msg;

import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.locks.ClientServerExchangeLockContext;

import java.util.Collection;


public interface ClientHandshakeMessage extends TCMessage {
  
  void setUUID(String uuid);
  
  String getUUID();
  
  String getName();
  
  void setName(String name);
  
  void setClientVersion(String v);

  String getClientVersion();

  void setClientPID(int pid);

  int getClientPID();

  void setDiagnosticClient(boolean isDiagnosticClient);

  boolean diagnosticClient();
  
  void setEnterpriseClient(boolean isEnterpriseClient);

  boolean enterpriseClient();

  long getLocalTimeMills();

  void addReconnectReference(ClientEntityReferenceContext context);

  Collection<ClientEntityReferenceContext> getReconnectReferences();

  void addResendMessage(ResendVoltronEntityMessage message);

  Collection<ResendVoltronEntityMessage> getResendMessages();
}
