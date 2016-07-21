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
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import java.util.Set;
import org.terracotta.entity.ClientDescriptor;


/**
 */
public class ServerEntityRequestImpl implements ServerEntityRequest {
  
  private final ServerEntityAction action;
  private final ClientID node;
  private final TransactionID transaction;
  private final TransactionID oldest;
  private final Set<NodeID> replicates;
  private final EntityDescriptor eid;

  public ServerEntityRequestImpl(EntityDescriptor descriptor, ServerEntityAction action, ClientID node, TransactionID transaction, TransactionID oldest, Set<NodeID> replicates) {
    this.eid = descriptor;
    this.action = action;
    this.node = node;
    this.transaction = transaction;
    this.oldest = oldest;
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
  public ClientDescriptor getSourceDescriptor() {
    return new ClientDescriptorImpl(node, eid);
  }

  @Override
  public Set<NodeID> replicateTo(Set<NodeID> passives) {
    return replicates;
  }

}
