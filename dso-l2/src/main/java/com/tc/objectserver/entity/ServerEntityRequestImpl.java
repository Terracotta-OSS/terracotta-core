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
package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
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
  private final Set<NodeID> replicates;
  private final ClientInstanceID cid;

  public ServerEntityRequestImpl(ClientInstanceID descriptor, ServerEntityAction action, ClientID node, TransactionID transaction, TransactionID oldest, boolean requiresReceived, Set<NodeID> replicates) {
    this.cid = descriptor;
    this.action = action;
    this.node = node;
    this.transaction = transaction;
    this.oldest = oldest;
    this.requiresReceived = requiresReceived;
    this.replicates = replicates;
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
  public Set<NodeID> replicateTo(Set<NodeID> passives) {
    // Note that we should be avoiding the decision to replicate messages at a higher-level so filter out any local-only
    //  operations.
    Assert.assertFalse((ServerEntityAction.LOCAL_FLUSH == this.action)
        || (ServerEntityAction.LOCAL_FLUSH_AND_SYNC == this.action));
    return this.replicates;
  }

}
