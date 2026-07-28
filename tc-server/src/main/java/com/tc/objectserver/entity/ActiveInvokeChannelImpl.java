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

import com.tc.util.concurrent.SetOnceFlag;
import java.util.function.Consumer;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.EntityResponse;

/**
 *
 */
public class ActiveInvokeChannelImpl<R extends EntityResponse> implements ActiveInvokeChannel<R> {

  private final Consumer<EntityResponse> account;
  private final Consumer<Exception> exception;
  private final Runnable retirementTrigger;
  private final SetOnceFlag closed = new SetOnceFlag();

  public ActiveInvokeChannelImpl(Consumer<EntityResponse> account, Consumer<Exception> exception, Runnable retirementTrigger) {
    this.account = account;
    this.exception = exception;
    this.retirementTrigger = retirementTrigger;
  }

  @Override
  public void sendResponse(R response) {
    if (!closed.isSet()) {
      account.accept(response);
    } else {
      throw new IllegalStateException("trying to send a response on a closed channel");
    }
  }

  @Override
  public void sendException(Exception response) {
    if (closed.attemptSet()) {
      exception.accept(response);
    } else {
      throw new IllegalStateException("trying to send an exception on a closed channel");
    }
  }

  @Override
  public void close() {
    if (closed.attemptSet()) {
      retirementTrigger.run();
    }
  }
}
