/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.entity;

import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityReceivedResponse;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;

import java.util.Optional;

import org.terracotta.exception.EntityException;


public abstract class AbstractServerEntityRequest implements ServerEntityRequest {
  private final ServerEntityAction action;
  private final TransactionID transaction;
  private final TransactionID oldest;
  private final NodeID  src;
  private final byte[]  payload;
  private final EntityDescriptor descriptor;
  private final boolean requiresReplication;
  
  private boolean done = false;

  public AbstractServerEntityRequest(EntityDescriptor descriptor, ServerEntityAction action, byte[] payload, TransactionID transaction, TransactionID oldest, NodeID src, boolean requiresReplication) {
    this.action = action;
    this.transaction = transaction;
    this.oldest = oldest;
    this.src = src;
    this.payload = payload;
    this.descriptor = descriptor;
    this.requiresReplication = requiresReplication;
  }
  
  public abstract Optional<MessageChannel> getReturnChannel();

  @Override
  public TransactionID getTransaction() {
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
  public boolean requiresReplication() {
    return this.requiresReplication;
  }

  @Override
  public NodeID getNodeID() {
    return src;
  }

  @Override
  public byte[] getPayload() {
    return payload;
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
  }  
  
  protected EntityDescriptor getEntityDescriptor() {
    return descriptor;
  }
  
  protected boolean isDone() {
    return done;
  }  
}
