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
package com.tc.objectserver.api;

import com.tc.exception.ServerException;
import com.tc.objectserver.entity.ActivePassiveAckWaiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 *
 */
public interface ResultCapture extends ServerEntityResponse {
  void message(byte[] message);
  void setWaitFor(Supplier<ActivePassiveAckWaiter> waiter);
  CompletionStage<Void> retired();

  static ResultCapture noop() {
    return new ResultCapture() {
      @Override
      public void message(byte[] message) {

      }

      public void setWaitFor(Supplier<ActivePassiveAckWaiter> waiter) {

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

      }

      @Override
      public void received() {

      }
    };
  }

  static ResultCapture chain(ResultCapture...list) {
    return new ResultCapture() {
      @Override
      public void message(byte[] message) {
        for (ResultCapture r : list) {
          r.message(message);
        }
      }

      public void setWaitFor(Supplier<ActivePassiveAckWaiter> waiter) {
        for (ResultCapture r : list) {
          r.setWaitFor(waiter);
        }
      }

      @Override
      public CompletionStage<Void> retired() {
        CompletionStage<Void> together = CompletableFuture.completedFuture(null);
        for (ResultCapture r : list) {
          together = together.thenCompose((n)->r.retired());
        }
        return together;
      }

      @Override
      public void complete() {
        for (ResultCapture r : list) {
          r.complete();
        }
      }

      @Override
      public void complete(byte[] value) {
        for (ResultCapture r : list) {
          r.complete(value);
        }
      }

      @Override
      public void failure(ServerException e) {
        for (ResultCapture r : list) {
          r.failure(e);
        }
      }

      @Override
      public void received() {
        for (ResultCapture r : list) {
          r.received();
        }
      }
    };
  }
}
