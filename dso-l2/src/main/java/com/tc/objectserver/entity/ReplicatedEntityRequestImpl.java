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

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import java.util.Optional;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.exception.EntityException;


/**
 * 
 * A replicated entity request will live on the passive.  This differs from ServerEntityRequests in that they do 
 * not directly communicate with clients so requestedAcks will not be acted upon.  If an entity is transitioned from 
 * passive to active mode, the client may connect to running replicated requests and will receive a new completed 
 * return from this entity
 */
public class ReplicatedEntityRequestImpl extends AbstractServerEntityRequest {
  
  private ClientDescriptor client;
  private Optional<MessageChannel> returnChannel;
  private byte[] returnValue = null;
  private EntityException failure = null;

  public ReplicatedEntityRequestImpl(EntityDescriptor descriptor, ServerEntityAction action, byte[] payload, TransactionID transaction, TransactionID oldest, NodeID nodes) {
    // This replication argument (the "true") is redundant, in this case.
    super(descriptor, action, payload, transaction, oldest, nodes, true);
  }

  @Override
  public synchronized void complete(byte[] value) {
    returnValue = value;
    super.complete(value);
  }

  @Override
  public synchronized void failure(EntityException e) {
    failure = e;
    super.failure(e); 
  }

  @Override
  public Optional<MessageChannel> getReturnChannel() {
    return returnChannel;
  }

  @Override
  public ClientDescriptor getSourceDescriptor() {
    return client;
  }

  @Override
  public boolean requiresReplication() {
    return false;
  }

//  TODO:  fix implementation once we decide what we want
  public synchronized void adoptOnFailover(ClientDescriptor client, Optional<MessageChannel> newChannel) {
    this.client = client;
    this.returnChannel = newChannel;
    if (isDone()) {
      if (returnValue != null) {
        super.complete(returnValue);
      } else if (failure != null) {
        super.failure(failure);
      } else {
        super.complete();
      }
    }
  }
}
