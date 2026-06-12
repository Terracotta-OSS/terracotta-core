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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */

public class DebugResultCapture implements ResultCapture {
  private static final Logger LOGGER = LoggerFactory.getLogger(DebugResultCapture.class);

  public DebugResultCapture() {

  }

  @Override
  public void setWaitFor(Supplier<ActivePassiveAckWaiter> waitFor) {

  }

  @Override
  public void waitForReceived() {

  }

  @Override
  public void received() {

  }

  @Override
  public void complete() {

  }

  @Override
  public void complete(byte[] value) {

  }

  @Override
  public void failure(ServerException ee) {

  }

  @Override
  public void message(byte[] m) {

  }

  @Override
  public CompletionStage<Void> retired() {
    return CompletableFuture.completedFuture(null);
  }
}
