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

import com.tc.exception.ServerException;
import com.tc.net.utils.L2Utils;
import com.tc.objectserver.api.ResultCapture;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 *
 * @author
 */
public class WaitingResultCapture implements ResultCapture {
  private volatile Supplier<ActivePassiveAckWaiter> setOnce;
  private final Future<Void> transactionOrderPersistenceFuture;
  private final boolean receiveRequired;

  public WaitingResultCapture(Future<Void> transactionOrderPersistenceFuture, boolean receiveRequired) {
    this.transactionOrderPersistenceFuture = transactionOrderPersistenceFuture;
    this.receiveRequired = receiveRequired;
  }

  @Override
  public CompletionStage<Void> retired() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void complete() {

  }

  @Override
  public void complete(byte[] value) {

  }

  @Override
  public void failure(ServerException e) {
    if (setOnce != null) {
      ActivePassiveAckWaiter waiter = setOnce.get();
      waiter.waitForCompleted();
    }
  }

  @Override
  public void received() {
    if (setOnce != null && this.receiveRequired) {
      setOnce.get().waitForReceived();
    }
    if(transactionOrderPersistenceFuture != null) {
      try {
        transactionOrderPersistenceFuture.get();
      } catch (InterruptedException ie) {
        L2Utils.handleInterrupted(null, ie);
      } catch (ExecutionException e) {
        throw new RuntimeException("Caught exception while persisting transaction order", e);
      }
    }
  }

  @Override
  public void message(byte[] message) {

  }


  @Override
  public void setWaitFor(Supplier<ActivePassiveAckWaiter> waiter) {
    this.setOnce = waiter;
  }
}
