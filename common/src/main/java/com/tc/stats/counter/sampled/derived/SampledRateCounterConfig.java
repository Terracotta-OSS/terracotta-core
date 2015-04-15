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
package com.tc.stats.counter.sampled.derived;

import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.SampledCounterConfig;

public class SampledRateCounterConfig extends SampledCounterConfig {

  private final long initialNumeratorValue;
  private final long initialDenominatorValue;

  public SampledRateCounterConfig(int intervalSecs, int historySize, boolean isResetOnSample) {
    this(intervalSecs, historySize, isResetOnSample, 0, 0);
  }

  public SampledRateCounterConfig(int intervalSecs, int historySize, boolean isResetOnSample,
                                  long initialNumeratorValue, long initialDenominatorValue) {
    super(intervalSecs, historySize, isResetOnSample, 0);
    this.initialNumeratorValue = initialNumeratorValue;
    this.initialDenominatorValue = initialDenominatorValue;
  }

  @Override
  public Counter createCounter() {
    SampledRateCounterImpl sampledRateCounter = new SampledRateCounterImpl(this);
    sampledRateCounter.setValue(initialNumeratorValue, initialDenominatorValue);
    return sampledRateCounter;
  }

}
