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

import com.tc.exception.ServerException;
import com.tc.objectserver.api.ResultCapture;
import com.tc.tracing.Trace;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 *
 */
 
public class NoopResultCapture implements ResultCapture {
  
  public NoopResultCapture() {
  }

  @Override
  public void setWaitFor(Supplier<ActivePassiveAckWaiter> waitFor) {

  }

  @Override
  public void waitForReceived() {

  }

  @Override
  public void received() {
    Trace.activeTrace().log("Received");
  }

  @Override
  public void complete() {
    Trace.activeTrace().log("Completed without result ");
  }  

  @Override
  public void complete(byte[] value) {
    if (Trace.isTraceEnabled()) {
      Trace.activeTrace().log("Completed with result of length " + value.length);
    }
  }

  @Override
  public void failure(ServerException ee) {
    if (Trace.isTraceEnabled()) {
      Trace.activeTrace().log("Failure - exception: " + ee.getLocalizedMessage());
    }
  }
  
  @Override
  public void message(byte[] message) {
    
  }

  @Override
  public CompletionStage<Void> retired() {
    Trace.activeTrace().log("Retired");
    return CompletableFuture.completedFuture(null);
  }
  
  
}
