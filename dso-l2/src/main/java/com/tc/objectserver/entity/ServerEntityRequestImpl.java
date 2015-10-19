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
 * Translated from Request in the entity package.  Provides payload transport through execution
 * and controls return of acks and completion to client.
 */
public class ServerEntityRequestImpl extends AbstractServerEntityRequest {

  protected final Optional<MessageChannel> returnChannel;

  public ServerEntityRequestImpl(EntityDescriptor descriptor, ServerEntityAction action, byte[] payload, 
      TransactionID transaction, TransactionID oldest, NodeID src, boolean requiresReplication, Optional<MessageChannel> returnChannel) {
    super(descriptor, action, payload, transaction, oldest, src, requiresReplication);
    this.returnChannel = returnChannel;
  }

  @Override
  public Optional<MessageChannel> getReturnChannel() {
    return returnChannel;
  }

  @Override
  public synchronized void complete(byte[] value) {
    if (isDone()) throw new AssertionError("Double-sending response");
    super.complete(value); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public synchronized void complete() {
    if (isDone()) throw new AssertionError("Double-sending response");
    super.complete(); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public synchronized void failure(EntityException e) {
    if (isDone()) throw new AssertionError("Double-sending response", e);
    super.failure(e); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public ClientDescriptor getSourceDescriptor() {
    EntityDescriptor entityDescriptor = getEntityDescriptor();
    return new ClientDescriptorImpl(getNodeID(), entityDescriptor);
  }    
}
