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
import com.tc.util.Assert;

import java.util.Optional;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.exception.EntityException;


/**
 * Translated from Request in the entity package.  Provides payload transport through execution
 * and controls return of acks and completion to client.
 */
public class ServerEntityRequestImpl extends AbstractServerEntityRequest {
  protected final Optional<MessageChannel> returnChannel;
  // TODO:  Using this flag is a bit of a hack but so is ServerEntityRequest.getConcurrencyKey so hopefully we can find a
  // less general way of asking about this so we won't need this flag to re-specialize it.
  private final boolean doesDeclareConcurrencyKey;

  // TODO:  Coalesce these constructors once we handle this doesDeclareConcurrencyKey in a better way.
  public ServerEntityRequestImpl(EntityDescriptor descriptor, ServerEntityAction action,  
      TransactionID transaction, TransactionID oldest, NodeID src, boolean requiresReplication, Optional<MessageChannel> returnChannel) {
    super(descriptor, action, transaction, oldest, src, requiresReplication);
    this.returnChannel = returnChannel;
    this.doesDeclareConcurrencyKey = false;
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
