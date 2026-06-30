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
import com.tc.objectserver.api.ResultCapture;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 *
 */

public class NoopResultCapture implements ResultCapture {

  private static final ResultCapture NOOP = new NoopResultCapture();
  private static final CompletionStage<Void> NOOP_RETIRED = CompletableFuture.completedFuture(null);

  public static ResultCapture noop() {
    return NOOP;
  }

  @Override
  public void message(byte[] message) {

  }

  @Override
  public void setWaitFor(Supplier<ActivePassiveAckWaiter> waiter) {

  }

  @Override
  public CompletionStage<Void> retired() {
    return NOOP_RETIRED;
  }

  @Override
  public void complete() {

  }

  @Override
  public void complete(byte[] value) {

  }

  @Override
  public void failure(ServerException e) {

  }

  @Override
  public void received() {

  }

}
