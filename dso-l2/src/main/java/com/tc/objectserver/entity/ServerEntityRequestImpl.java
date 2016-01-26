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
  private final EntityDescriptor descriptor;
  protected final Optional<MessageChannel> returnChannel;

  public ServerEntityRequestImpl(EntityDescriptor descriptor, ServerEntityAction action,  
      TransactionID transaction, TransactionID oldest, ClientID src, boolean requiresReplication, Optional<MessageChannel> returnChannel) {
    super(action, transaction, oldest, src, requiresReplication);
    this.descriptor = descriptor;
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
    return new ClientDescriptorImpl(getNodeID(), this.descriptor);
  }
}
