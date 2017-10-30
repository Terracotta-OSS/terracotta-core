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

import java.util.function.Consumer;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.EntityResponse;

/**
 *
 */
public class ActiveInvokeChannelImpl<R extends EntityResponse> implements ActiveInvokeChannel<R> {
  
  private final Consumer<R> account;
  private final Consumer<Exception> exception;
  private final Runnable retirementTrigger;

  public ActiveInvokeChannelImpl(Consumer<R> account, Consumer<Exception> exception, Runnable retirementTrigger) {
    this.account = account;
    this.exception = exception;
    this.retirementTrigger = retirementTrigger;
  }


  @Override
  public void sendResponse(R response) {
    account.accept(response);
  }

  @Override
  public void sendException(Exception response) {
    exception.accept(response);
  }

  @Override
  public void close() {
    retirementTrigger.run();
  }
  
}
