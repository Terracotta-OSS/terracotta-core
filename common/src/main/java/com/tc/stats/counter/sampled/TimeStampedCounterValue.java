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

import java.io.Serializable;

/**
 * A counter value at a particular time instance
 */
public class TimeStampedCounterValue implements Serializable {
  private final long counterValue;
  private final long timestamp;

  public TimeStampedCounterValue(long timestamp, long value) {
    this.timestamp = timestamp;
    this.counterValue = value;
  }

  public long getCounterValue() {
    return this.counterValue;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  @Override
  public String toString() {
    return "value: " + this.counterValue + ", timestamp: " + this.timestamp;
  }

}
