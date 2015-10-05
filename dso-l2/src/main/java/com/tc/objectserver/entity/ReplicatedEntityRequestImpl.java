/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.entity;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import java.util.Optional;
import org.terracotta.entity.ClientDescriptor;


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
  private Exception failure = null;

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
  public synchronized void failure(Exception e) {
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
