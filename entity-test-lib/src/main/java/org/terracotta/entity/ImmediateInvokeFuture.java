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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.entity;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.terracotta.exception.EntityException;


/**
 * An implementation of InvokeFuture, used for testing, which returns the future, immediately.
 * 
 * @param <T> The underlying type wrapped by the instance
 */
public class ImmediateInvokeFuture<T> implements InvokeFuture<T> {
  final T result;
  final EntityException error;

  public ImmediateInvokeFuture(T result, EntityException error) {
    this.result = result;
    this.error = error;
  }
  
  @Override
  public boolean isDone() {
    return true;
  }
  @Override
  public T get() throws InterruptedException, EntityException {
    if (null != error) {
      throw error;
    } else {
      return result;
    }
  }
  @Override
  public T getWithTimeout(long timeout, TimeUnit unit) throws InterruptedException, EntityException, TimeoutException {
    if (null != error) {
      throw error;
    } else {
      return result;
    }
  }
  @Override
  public void interrupt() {
  }
}
