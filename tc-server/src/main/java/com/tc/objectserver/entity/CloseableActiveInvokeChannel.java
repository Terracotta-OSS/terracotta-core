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

import com.tc.util.concurrent.SetOnceFlag;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.EntityResponse;

/**
 *
 */
public class CloseableActiveInvokeChannel<R extends EntityResponse> implements ActiveInvokeChannel<R> {
  ActiveInvokeChannelImpl delegate;
  SetOnceFlag closed = new SetOnceFlag();

  public CloseableActiveInvokeChannel(ActiveInvokeChannelImpl delegate) {
    this.delegate = delegate;
  }

  @Override
  public void sendResponse(R r) {
    if (!closed.isSet()) {
      delegate.sendResponse(r);
    } else {
      throw new IllegalStateException("already closed");
    }
  }

  @Override
  public void sendException(Exception excptn) {
    if (!closed.isSet()) {
      delegate.sendException(excptn);
    } else {
      throw new IllegalStateException("already closed");
    }
  }

  @Override
  public void close() {
    if (closed.attemptSet()) {
      delegate.close();
    }
  }
}
