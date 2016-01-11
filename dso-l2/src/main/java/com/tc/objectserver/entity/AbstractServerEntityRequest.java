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

import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityReceivedResponse;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import java.util.Collections;

import java.util.Optional;
import java.util.Set;

import org.terracotta.exception.EntityException;


public abstract class AbstractServerEntityRequest implements ServerEntityRequest {
  private final ServerEntityAction action;
  private final TransactionID transaction;
  private final TransactionID oldest;
  private final ClientID  src;
  private final EntityDescriptor descriptor;
  private final boolean requiresReplication;
  
  private boolean done = false;

  public AbstractServerEntityRequest(EntityDescriptor descriptor, ServerEntityAction action, TransactionID transaction, TransactionID oldest, ClientID src, boolean requiresReplication) {
    this.action = action;
    this.transaction = transaction;
    this.oldest = oldest;
    this.src = src;
    this.descriptor = descriptor;
    this.requiresReplication = requiresReplication;
  }
  
  public abstract Optional<MessageChannel> getReturnChannel();

  @Override
  public TransactionID getTransaction() {
    if (transaction == null) {
      return TransactionID.NULL_ID;
    }
    return transaction;
  }
  
  @Override
  public TransactionID getOldestTransactionOnClient() {
    if (oldest == null) {
      return TransactionID.NULL_ID;
    }
    return oldest;
  }

  @Override
  public Set<NodeID> replicateTo(Set<NodeID> current) {
    return requiresReplication ? current : Collections.emptySet();
  }

  @Override
  public ClientID getNodeID() {
    return src;
  }

  @Override
  public ServerEntityAction getAction() {
    return action;
  }
  
  @Override
  public synchronized void failure(EntityException e) {
    if (isDone()) throw new AssertionError("Error after successful complete");
    getReturnChannel().ifPresent(channel -> {
      VoltronEntityAppliedResponse message = (VoltronEntityAppliedResponse) channel.createMessage(TCMessageType.VOLTRON_ENTITY_APPLIED_RESPONSE);
      message.setFailure(transaction, e);
      message.send();
    });
    done = true;
    this.notifyAll();
  }

  @Override
  public synchronized void received() {
    getReturnChannel().ifPresent(channel -> {
      VoltronEntityReceivedResponse message = (VoltronEntityReceivedResponse) channel.createMessage(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE);
      message.setTransactionID(transaction);
      message.send();
    });
  }
  
  @Override
  public synchronized void complete() {
    getReturnChannel().ifPresent(channel -> {
      VoltronEntityAppliedResponse actionResponse = (VoltronEntityAppliedResponse) channel.createMessage(TCMessageType.VOLTRON_ENTITY_APPLIED_RESPONSE);
      switch (action) {
        case CREATE_ENTITY:
        case DESTROY_ENTITY:
        case RELEASE_ENTITY:
        case DOES_EXIST:
          // In these cases, we just return an empty success to acknowledge that they completed.
          actionResponse.setSuccess(transaction, new byte[0]);
          actionResponse.send();
          break;
        default:
          // Unknown action completion type.
          throw new IllegalArgumentException("Unexpected action in complete() " + action);
      }
    });
    done = true;
    this.notifyAll();
  }
  
  @Override
  public synchronized void complete(byte[] value) {
    getReturnChannel().ifPresent(channel -> {
      switch (action) {
        case INVOKE_ACTION:
        case FETCH_ENTITY:
          VoltronEntityAppliedResponse actionResponse = (VoltronEntityAppliedResponse) channel.createMessage(TCMessageType.VOLTRON_ENTITY_APPLIED_RESPONSE);
          actionResponse.setSuccess(transaction, value);
          actionResponse.send();
          break;
        default:
          throw new IllegalArgumentException("Unexpected action in complete(byte[]) " + action);
      }
    });
    done = true;
    this.notifyAll();
  }  
  
  protected EntityDescriptor getEntityDescriptor() {
    return descriptor;
  }
  
  protected boolean isDone() {
    return done;
  }  
  
  public synchronized void waitForDone() {
    boolean interrupted = false;
    while (!done) {
      try {
        wait();
      } catch (InterruptedException ie) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
