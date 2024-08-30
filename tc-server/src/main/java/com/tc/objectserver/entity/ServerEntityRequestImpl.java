/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.util.Assert;

import java.util.Set;


public class ServerEntityRequestImpl implements ServerEntityRequest {
  
  private final ServerEntityAction action;
  private final ClientID node;
  private final TransactionID transaction;
  private final TransactionID oldest;
  private final boolean requiresReceived;
  private final ClientInstanceID cid;

  public ServerEntityRequestImpl(ClientInstanceID descriptor, ServerEntityAction action, ClientID node, TransactionID transaction, TransactionID oldest, boolean requiresReceived) {
    this.cid = descriptor;
    this.action = action;
    this.node = node;
    this.transaction = transaction;
    this.oldest = oldest;
    this.requiresReceived = requiresReceived;
  }

  @Override
  public ServerEntityAction getAction() {
    return action;
  }

  @Override
  public ClientID getNodeID() {
    return node;
  }

  @Override
  public TransactionID getTransaction() {
    return transaction;
  }

  @Override
  public TransactionID getOldestTransactionOnClient() {
    return oldest;
  }

  @Override
  public ClientInstanceID getClientInstance() {
    return  cid;
  }

  @Override
  public boolean requiresReceived() {
    return requiresReceived;
  }
 
  @Override
  public Set<SessionID> replicateTo(Set<SessionID> passives) {
    // Note that we should be avoiding the decision to replicate messages at a higher-level so filter out any local-only
    //  operations.
    Assert.assertFalse((ServerEntityAction.LOCAL_FLUSH == this.action)
        || (ServerEntityAction.LOCAL_FLUSH_AND_SYNC == this.action));
    return passives;
  }

}
