/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.entity;

import com.tc.entity.VoltronEntityResponse;
import com.tc.exception.ServerException;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.objectserver.api.ResultCapture;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.util.Assert;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Translated from Request in the entity package.  Provides payload transport through execution
 * and controls return of acks and completion to client.
 */
public class ServerEntityRequestResponse extends AbstractServerEntityRequestResponse implements ResultCapture {
  protected final Supplier<Optional<MessageChannel>> returnChannel;
  // We only track whether this is replicated to know that we should reject retire acks.
  private final boolean isReplicatedMessage;
  
  private Supplier<ActivePassiveAckWaiter> waiter;

  public ServerEntityRequestResponse(ServerEntityRequest request, 
      Consumer<VoltronEntityResponse> sender,
      Supplier<Optional<MessageChannel>> returnChannel, 
      Consumer<byte[]> completion, Consumer<ServerException> exception, boolean isReplicatedMessage) {
    super(request, sender, completion, exception);
    this.returnChannel = returnChannel;
    this.isReplicatedMessage = isReplicatedMessage;
  }

  @Override
  public Optional<MessageChannel> getReturnChannel() {
    return returnChannel.get();
  }

  @Override
  public void message(byte[] message) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setWaitFor(Supplier<ActivePassiveAckWaiter> waiter) {
    this.waiter = waiter;
  }

  @Override
  public void waitForReceived() {
    this.waiter.get().waitForReceived();
  }

  @Override
  public synchronized void complete(byte[] value) {
    if (isComplete()) throw new AssertionError("Double-sending response " + this.getAction());
    if (value == null) {
      super.complete();
    } else {
      super.complete(value); 
    }
  }

  @Override
  public synchronized void complete() {
    if (isComplete()) throw new AssertionError("Double-sending response " + this.getAction());
    super.complete();
  }

  @Override
  public synchronized void failure(ServerException e) {
    if (isComplete()) throw new AssertionError("Double-sending response " + this.getAction(), e);
    super.failure(e); 
  }
 
  @Override
  public synchronized CompletionStage<Void> retired() {
    // Replicated messages are never retired.
    Assert.assertFalse(this.isReplicatedMessage);
    // We can only send the retire, once.
    if (isRetired()) {
      throw new AssertionError("Double-sending retire " + this.getAction());
    }
    return super.retired();
  }
}
