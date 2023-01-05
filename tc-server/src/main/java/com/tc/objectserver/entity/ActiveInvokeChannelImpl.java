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

import com.tc.util.concurrent.SetOnceFlag;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.terracotta.entity.EntityResponse;

/**
 *
 */
public class ActiveInvokeChannelImpl {
  
  private final Consumer<EntityResponse> account;
  private final Consumer<Exception> exception;
  private final Runnable retirementTrigger;
  private final SetOnceFlag closed = new SetOnceFlag();
  private final AtomicInteger references = new AtomicInteger(1);

  public ActiveInvokeChannelImpl(Consumer<EntityResponse> account, Consumer<Exception> exception, Runnable retirementTrigger) {
    this.account = account;
    this.exception = exception;
    this.retirementTrigger = retirementTrigger;
  }

  public void sendResponse(EntityResponse response) {
    if (!closed.isSet()) {
      account.accept(response);
    } else {
      throw new IllegalStateException("trying to send a response on a closed channel");
    }
  }

  public void sendException(Exception response) {
    if (closed.attemptSet()) {
      exception.accept(response);
    } else {
      throw new IllegalStateException("trying to send an exception on a closed channel");
    }
  }

  public void close() {
    if (references.decrementAndGet() == 0) {
      if (closed.attemptSet()) {
        retirementTrigger.run();
      }
    } else {
      
    }
  }
  
  public boolean reference() {
    return (references.getAndIncrement() != 0);
  }
}
