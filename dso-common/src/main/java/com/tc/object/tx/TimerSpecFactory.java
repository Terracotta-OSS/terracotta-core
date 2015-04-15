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
package com.tc.object.tx;

import com.tc.util.Assert;

public class TimerSpecFactory {
  public TimerSpec newTimerSpec(int waitArgCount, long millis, int nanos) {
    switch (waitArgCount) {
      case 2: {
        return new TimerSpec(millis, nanos);
      }
      case 1: {
        return new TimerSpec(millis);
      }
      case 0: {
        return new TimerSpec();
      }
      default: {
        throw Assert.failure("Invalid wait argument count: " + waitArgCount);
      }
    }
  }
}
