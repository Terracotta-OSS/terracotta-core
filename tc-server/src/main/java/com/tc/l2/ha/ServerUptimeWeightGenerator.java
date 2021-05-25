/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
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
