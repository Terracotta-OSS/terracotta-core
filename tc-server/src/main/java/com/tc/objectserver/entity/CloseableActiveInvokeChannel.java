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
    } else {
      throw new IllegalStateException("already closed");
    }
  }
}
