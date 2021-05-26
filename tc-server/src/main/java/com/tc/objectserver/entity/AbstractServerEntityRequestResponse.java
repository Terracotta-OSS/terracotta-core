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
import com.tc.entity.VoltronEntityResponse;
import com.tc.entity.VoltronEntityRetiredResponse;
import com.tc.exception.ServerException;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.utils.L2Utils;
import com.tc.object.ClientInstanceID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.Retiree;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.api.ServerEntityResponse;
import com.tc.util.Assert;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;


public abstract class AbstractServerEntityRequestResponse implements ServerEntityRequest, ServerEntityResponse, Retiree {
  private final ServerEntityRequest request;
  private final Consumer<byte[]> complete;
  private final Consumer<ServerException> fail;
  
  private final Consumer<VoltronEntityResponse> messageSender;
    
  private boolean isComplete = false;
  private boolean isRetired = false;

  private volatile Future<Void> transactionOrderPersistenceFuture;
  
  public AbstractServerEntityRequestResponse(ServerEntityRequest action, Consumer<VoltronEntityResponse> messageSender, Consumer<byte[]> complete, Consumer<ServerException> fail) {
    this.request = action;
    this.messageSender = messageSender;
    this.complete = complete;
    this.fail = fail;
  }
  
  public abstract Optional<MessageChannel> getReturnChannel();

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
  public Set<SessionID> replicateTo(Set<SessionID> current) {
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
  public void failure(ServerException e) {
    if (!this.getNodeID().isNull()) {
      getReturnChannel().ifPresent(channel -> {
        VoltronEntityAppliedResponse message = (VoltronEntityAppliedResponse)channel.createMessage(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE);
        message.setFailure(request.getTransaction(), e);
        messageSender.accept(message);
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
        } catch (InterruptedException ie) {
          L2Utils.handleInterrupted(null, ie);
        } catch (ExecutionException e) {
          throw new RuntimeException("Caught exception while persisting transaction order", e);
        }
      }
      message.setTransactionID(request.getTransaction());
      messageSender.accept(message);
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
            messageSender.accept(actionResponse);
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
            messageSender.accept(actionResponse);
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
  public CompletionStage<Void> retired() {
    Assert.assertTrue("Double-retire", !isRetired());
    
    getReturnChannel().ifPresent(channel -> {
      VoltronEntityRetiredResponse response = (VoltronEntityRetiredResponse) channel.createMessage(TCMessageType.VOLTRON_ENTITY_RETIRED_RESPONSE);
      response.setTransactionID(request.getTransaction());
      messageSender.accept(response);
    });
    this.isRetired = true;
    
    return CompletableFuture.completedFuture(null);
  }
  
  protected boolean isComplete() {
    return this.isComplete;
  }  
  
  protected boolean isRetired() {
    return this.isRetired;
  }  
}
