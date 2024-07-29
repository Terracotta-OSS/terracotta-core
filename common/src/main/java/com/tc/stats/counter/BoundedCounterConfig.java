/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.stats.counter;

public class BoundedCounterConfig extends SimpleCounterConfig {

  private final long min;
  private final long max;

  public BoundedCounterConfig(long initialValue, long min, long max) {
    super(initialValue);
    this.min = min;
    this.max = max;
  }

  public long getMinBound() {
    return min;
  }

  public long getMaxBound() {
    return max;
  }

  @Override
  public Counter createCounter() {
    return new BoundedCounter(getInitialValue(), min, max);
  }
}
