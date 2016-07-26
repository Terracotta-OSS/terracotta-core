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
import com.tc.util.Assert;

import java.util.Optional;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.exception.EntityException;


/**
 * Translated from Request in the entity package.  Provides payload transport through execution
 * and controls return of acks and completion to client.
 */
public class ServerEntityRequestResponse extends AbstractServerEntityRequestResponse {
  private final EntityDescriptor descriptor;
  protected final Optional<MessageChannel> returnChannel;
  // We only track whether this is replicated to know that we should reject retire acks.
  private final boolean isReplicatedMessage;
  private boolean autoRetire = false;

  public ServerEntityRequestResponse(EntityDescriptor descriptor, ServerEntityAction action,  
      TransactionID transaction, TransactionID oldest, ClientID src, boolean requiresReplication, Optional<MessageChannel> returnChannel, boolean isReplicatedMessage) {
    super(action, transaction, oldest, src, requiresReplication);
    this.descriptor = descriptor;
    this.returnChannel = returnChannel;
    this.isReplicatedMessage = isReplicatedMessage;
  }

  @Override
  public Optional<MessageChannel> getReturnChannel() {
    return returnChannel;
  }

  @Override
  public synchronized void complete(byte[] value) {
    if (isComplete()) throw new AssertionError("Double-sending response " + this.getAction());
    if (value == null) {
      super.complete();
    } else {
      super.complete(value); //To change body of generated methods, choose Tools | Templates.
    }
    autoRetire();
  }

  @Override
  public synchronized void complete() {
    if (isComplete()) throw new AssertionError("Double-sending response " + this.getAction());
    super.complete(); //To change body of generated methods, choose Tools | Templates.
    autoRetire();
  }

  @Override
  public synchronized void failure(EntityException e) {
    if (isComplete()) throw new AssertionError("Double-sending response " + this.getAction(), e);
    super.failure(e); //To change body of generated methods, choose Tools | Templates.
    autoRetire();
  }

  public void setAutoRetire() {
    this.autoRetire = true;
  }

  private void autoRetire() {
    if (autoRetire) {
      this.retired();
    }
  }

  @Override
  public synchronized void retired() {
    // Replicated messages are never retired.
    Assert.assertFalse(this.isReplicatedMessage);
    // We can only send the retire, once.
    if (isRetired()) {
      throw new AssertionError("Double-sending retire " + this.getAction());
    }
    super.retired();
  }

  @Override
  public ClientDescriptor getSourceDescriptor() {
    return new ClientDescriptorImpl(getNodeID(), this.descriptor);
  }
}
