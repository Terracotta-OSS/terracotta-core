/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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

import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityReceivedResponse;
import com.tc.entity.VoltronEntityResponse;
import com.tc.entity.VoltronEntityRetiredResponse;
import com.tc.exception.ServerException;
import com.tc.net.protocol.tcm.TCAction;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ResultCapture;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @author
 */
public class NetworkServerEntityResponse implements ResultCapture {
  private final Function<TCMessageType, TCAction> messageCreate;
  private final Consumer<VoltronEntityResponse> messageSender;
  private final TransactionID  transaction;
  private boolean complete = false;
  private boolean retired = false;

  public NetworkServerEntityResponse(TransactionID transaction, Function<TCMessageType, TCAction> mCreate, Consumer<VoltronEntityResponse> messageSender) {
    Objects.requireNonNull(mCreate);
    Objects.requireNonNull(messageSender);
    this.transaction = transaction;
    this.messageCreate = mCreate;
    this.messageSender = messageSender;
  }

  @Override
  public void message(byte[] message) {

  }

  @Override
  public void setWaitFor(Supplier<ActivePassiveAckWaiter> waiter) {

  }

  @Override
  public void waitForReceived() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void complete() {
    complete(new byte[0]);
  }

  @Override
  public void complete(byte[] value) {
    VoltronEntityAppliedResponse actionResponse = (VoltronEntityAppliedResponse) messageCreate.apply(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE);
    if (actionResponse != null) {
      actionResponse.setSuccess(transaction, value);
      messageSender.accept(actionResponse);
    }

    this.complete = true;
  }

  @Override
  public void failure(ServerException e) {
    VoltronEntityAppliedResponse message = (VoltronEntityAppliedResponse) messageCreate.apply(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE);
    if (message != null) {
      message.setFailure(transaction, e);
      messageSender.accept(message);
    }
    this.complete = true;
  }

  @Override
  public void received() {
    VoltronEntityReceivedResponse message = (VoltronEntityReceivedResponse) messageCreate.apply(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE);
    if (message != null) {
      message.setTransactionID(transaction);
      messageSender.accept(message);
    }
  }

  @Override
  public CompletionStage<Void> retired() {
    VoltronEntityRetiredResponse response = (VoltronEntityRetiredResponse) messageCreate.apply(TCMessageType.VOLTRON_ENTITY_RETIRED_RESPONSE);
    if (response != null) {
      response.setTransactionID(transaction);
      messageSender.accept(response);
    }
    this.retired = true;
    return CompletableFuture.completedFuture(null);
  }
}
