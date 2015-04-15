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
