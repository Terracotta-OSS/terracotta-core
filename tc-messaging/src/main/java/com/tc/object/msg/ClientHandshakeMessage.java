/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object.msg;

import com.tc.entity.ResendVoltronEntityMessage;

import java.util.Collection;
import com.tc.net.protocol.tcm.TCAction;


public interface ClientHandshakeMessage extends TCAction {
  
  void setReconnect(boolean isReconnect);
  
  boolean isReconnect();
  
  void setUUID(String uuid);
  
  String getUUID();
  
  String getName();
  
  void setName(String name);
  
  void setClientVersion(String v);

  String getClientVersion();

  void setClientRevision(String v);

  String getClientRevision();

  void setClientPID(int pid);

  int getClientPID();

  long getLocalTimeMills();
  
  String getClientAddress();

  void addReconnectReference(ClientEntityReferenceContext context);

  Collection<ClientEntityReferenceContext> getReconnectReferences();

  void addResendMessage(ResendVoltronEntityMessage message);

  Collection<ResendVoltronEntityMessage> getResendMessages();
}
