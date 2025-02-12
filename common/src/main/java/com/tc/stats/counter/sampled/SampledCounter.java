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
package com.tc.stats.counter.sampled;

import com.tc.stats.counter.Counter;


public interface SampledCounter extends Counter {
  
  void shutdown();
  
  TimeStampedCounterValue getMostRecentSample();

  long getAndReset();
  
  public final static SampledCounter NULL_SAMPLED_COUNTER = new SampledCounter() {

    @Override
    public TimeStampedCounterValue getMostRecentSample() {
      return null;
    }

    @Override
    public void shutdown() {
      //
    }

    @Override
    public long decrement() {
      return 0;
    }

    @Override
    public long decrement(long amount) {
      return 0;
    }

    @Override
    public long getAndSet(long newValue) {
      return 0;
    }

    @Override
    public long getValue() {
      return 0;
    }

    @Override
    public long increment() {
      return 0;
    }

    @Override
    public long increment(long amount) {
      return 0;
    }

    @Override
    public void setValue(long newValue) {
      //
    }

    @Override
    public long getAndReset() {
      return 0;
    }
  };
  
}
