/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.stats.counter;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple counter implementation
 */
public class CounterImpl implements Counter, Serializable {
  private AtomicLong value;

  public CounterImpl() {
    this(0L);
  }

  public CounterImpl(long initialValue) {
    this.value = new AtomicLong(initialValue);
  }

  @Override
  public long increment() {
    return value.incrementAndGet();
  }

  @Override
  public long decrement() {
    return value.decrementAndGet();
  }

  @Override
  public long getAndSet(long newValue) {
    return value.getAndSet(newValue);
  }

  @Override
  public long getValue() {
    return value.get();
  }

  @Override
  public long increment(long amount) {
    return value.addAndGet(amount);
  }

  @Override
  public long decrement(long amount) {
    return value.addAndGet(amount * -1);
  }

  @Override
  public void setValue(long newValue) {
    value.set(newValue);
  }

}
