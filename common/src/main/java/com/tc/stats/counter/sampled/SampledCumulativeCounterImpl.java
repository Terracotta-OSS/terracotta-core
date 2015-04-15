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
package com.tc.stats.counter.sampled;

import java.util.concurrent.atomic.AtomicLong;

public class SampledCumulativeCounterImpl extends SampledCounterImpl implements SampledCumulativeCounter {

  private AtomicLong cumulativeCount;

  public SampledCumulativeCounterImpl(SampledCounterConfig config) {
    super(config);
    cumulativeCount = new AtomicLong(config.getInitialValue());
  }

  @Override
  public long getCumulativeValue() {
    if (resetOnSample) {
      return cumulativeCount.get();
    } else {
      return getValue();
    }
  }

  @Override
  public long decrement() {
    cumulativeCount.decrementAndGet();
    return super.decrement();
  }

  @Override
  public long decrement(long amount) {
    cumulativeCount.addAndGet(amount * -1);
    return super.decrement(amount);
  }

  @Override
  public long increment() {
    cumulativeCount.incrementAndGet();
    return super.increment();
  }

  @Override
  public long increment(long amount) {
    cumulativeCount.addAndGet(amount);
    return super.increment(amount);
  }

}
