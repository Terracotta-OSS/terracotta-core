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

import java.util.concurrent.atomic.AtomicInteger;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.EntityResponse;

/**
 *
 */
public class RefCountingActiveInvokeChannel<R extends EntityResponse> implements ActiveInvokeChannel<R> {
  private final ActiveInvokeChannel<R> delegate;
  private final AtomicInteger references = new AtomicInteger(1);

  public RefCountingActiveInvokeChannel(ActiveInvokeChannel<R> delegate) {
    this.delegate = delegate;
  }

  public int reference() {
    return references.getAndIncrement();
  }

  @Override
  public void sendResponse(R r) {
    delegate.sendResponse(r);
  }

  @Override
  public void sendException(Exception excptn) {
    delegate.sendException(excptn);
  }

  @Override
  public void close() {
    if (references.decrementAndGet() == 0) {
      delegate.close();
    }
  }
}
