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
import com.tc.entity.VoltronEntityRetiredResponse;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.Retiree;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.api.ServerEntityResponse;
import com.tc.util.Assert;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.terracotta.exception.EntityException;


public abstract class AbstractServerEntityRequestResponse implements ServerEntityRequest, ServerEntityResponse, Retiree {
  private final ServerEntityRequest request;
  private final Consumer<byte[]> complete;
  private final Consumer<EntityException> fail;
    
  private boolean isComplete = false;
  private boolean isRetired = false;
  private boolean alsoRetire = false;

  private volatile Future<Void> transactionOrderPersistenceFuture;
  
  public AbstractServerEntityRequestResponse(ServerEntityRequest action, Consumer<byte[]> complete, Consumer<EntityException> fail) {
    this.request = action;
    this.complete = complete;
    this.fail = fail;
  }
  
  public abstract Optional<MessageChannel> getReturnChannel();

  public final void autoRetire(boolean auto) {
    this.alsoRetire = auto;
  }

  @Override
  public ClientInstanceID getClientInstance() {
    return request.getClientInstance();
  }

  @Override
  public boolean requiresReceived() {
    return request.requiresReceived();
  }
  
  @Override
  public TransactionID getTransaction() {
    return request.getTransaction();
  }
  
  @Override
  public TransactionID getOldestTransactionOnClient() {
    return request.getOldestTransactionOnClient();
  }

  @Override
  public Set<NodeID> replicateTo(Set<NodeID> current) {
    return request.replicateTo(current);
  }

  @Override
  public String getTraceID() {
    return request.getTraceID();
  }
    
  @Override
  public ClientID getNodeID() {
    return request.getNodeID();
  }
      
  @Override
  public ServerEntityAction getAction() {
    return request.getAction();
  }
     
  @Override
  public void failure(EntityException e) {
    if (!this.getNodeID().isNull()) {
      getReturnChannel().ifPresent(channel -> {
        VoltronEntityAppliedResponse message = (VoltronEntityAppliedResponse) channel.createMessage(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE);
        message.setFailure(request.getTransaction(), e);
        message.send();
      });
      this.isComplete = true;
    }
    if (fail != null) {
      fail.accept(e);
    }
  }

  @Override
  public synchronized void received() {
    getReturnChannel().ifPresent(channel -> {
      VoltronEntityReceivedResponse message = (VoltronEntityReceivedResponse) channel.createMessage(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE);
      if(transactionOrderPersistenceFuture != null) {
        try {
          transactionOrderPersistenceFuture.get();
        } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException("Caught exception while persisting transaction order", e);
        }
      }
      message.setTransactionID(request.getTransaction());
      message.send();
    });
  }
  
  @Override
  public void complete() {
    if (!this.getNodeID().isNull()) {
      getReturnChannel().ifPresent(channel -> {
        VoltronEntityAppliedResponse actionResponse = (VoltronEntityAppliedResponse) channel.createMessage(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE);
        switch (request.getAction()) {
          case DISCONNECT_CLIENT:
            // do nothing, really shouldn't happen because this client is gone but maybe if there is 
            // some large delay in cleanup, this can occur
            break;
          case CREATE_ENTITY:
          case DESTROY_ENTITY:
          case RELEASE_ENTITY:
          case FETCH_ENTITY:
            // In these cases, we just return an empty success to acknowledge that they completed.
            actionResponse.setSuccess(request.getTransaction(), new byte[0]);
            actionResponse.send();

            break;
          default:
            // Unknown action completion type.
            throw new IllegalArgumentException("Unexpected action in complete() " + request.getAction());
        }
      });
      this.isComplete = true;
    }
    if (complete != null) {
      complete.accept(null);
    }
  }
  
  @Override
  public void complete(byte[] value) {
    if (!this.getNodeID().isNull()) {
      getReturnChannel().ifPresent(channel -> {
        switch (request.getAction()) {
          case DISCONNECT_CLIENT:
            // do nothing, really shouldn't happen because this client is gone but maybe if there is 
            // some large delay in cleanup, this can occur
            break;
          case INVOKE_ACTION:
          case FETCH_ENTITY:
          case RECONFIGURE_ENTITY:
            VoltronEntityAppliedResponse actionResponse = (VoltronEntityAppliedResponse) channel.createMessage(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE);
            actionResponse.setSuccess(request.getTransaction(), value);
            actionResponse.send();
            break;
          default:
            throw new IllegalArgumentException("Unexpected action in complete(byte[]) " + request.getAction());
        }
      });
      this.isComplete = true;
    }
    if (complete != null) {
      complete.accept(value);
    }
  }

  public void setTransactionOrderPersistenceFuture(Future<Void> transactionOrderPersistenceFuture) {
    this.transactionOrderPersistenceFuture = transactionOrderPersistenceFuture;
  }
  
  @Override
  public void retired() {
    Assert.assertTrue("Double-retire", !isRetired());
    
    getReturnChannel().ifPresent(channel -> {
      VoltronEntityRetiredResponse response = (VoltronEntityRetiredResponse) channel.createMessage(TCMessageType.VOLTRON_ENTITY_RETIRED_RESPONSE);
      response.setTransactionID(request.getTransaction());
      response.send();
    });
    this.isRetired = true;
  }
  
  protected boolean isComplete() {
    return this.isComplete;
  }  
  
  protected boolean isRetired() {
    return this.isRetired;
  }  
}
