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
package com.tc.l2.ha;

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import java.util.concurrent.TimeUnit;


public class ServerUptimeWeightGenerator implements WeightGenerator {
  private final long startMillis;
  private final boolean isAvailable;

  public ServerUptimeWeightGenerator(boolean isAvailable) {
    this.startMillis = System.currentTimeMillis();
    this.isAvailable = isAvailable;
  }

  @Override
  public long getWeight() {
  //  calculate the seconds of uptime and convert back to millis for backwards 
  // compatibility.  (i.e. 6055ms of uptime is converted to 6000ms of uptime)
    return TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - this.startMillis));
  }

  @Override
  public boolean isVerificationWeight() {
    return false;
  }
}
