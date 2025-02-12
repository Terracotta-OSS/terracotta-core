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
