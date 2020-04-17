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
 */
package com.tc.objectserver.api;

import com.tc.net.ClientID;

import com.tc.object.ClientInstanceID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import java.util.Set;


public interface ServerEntityRequest {

  ServerEntityAction getAction();
/**
 * the source of this request.  Always a client.
 * @return origin of the request
 */
  ClientID getNodeID();
  
  TransactionID getTransaction();
  
  TransactionID getOldestTransactionOnClient();
  /**
   * The descriptor referring to the specific client-side object instance which issued the request.
   * @return 
   */
  ClientInstanceID getClientInstance();
  
  boolean requiresReceived();
/**
 * Provide the nodes which need to be replicated to for this request
 * @param passives current set of passive nodes
 * @return the passives that this request needs to be replicated to
 */  
  Set<SessionID> replicateTo(Set<SessionID> passives);

  default String getTraceID() {
    return getNodeID().toLong() + ":" + getTransaction().toLong();
  }
}
