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
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.ConsistencyManagerImpl;


public class BlockTimeWeightGenerator implements WeightGenerator {
  private final ConsistencyManagerImpl consistency;

  public BlockTimeWeightGenerator(ConsistencyManager consistency) {
    this.consistency = consistency instanceof ConsistencyManagerImpl ? (ConsistencyManagerImpl)consistency : null;
  }

  @Override
  public long getWeight() {
    long currentTime = System.currentTimeMillis();
    // favors in the following order
    // non-blocking consistency (Positive long, consistency manager returns Long.MAX_VALUE)
    // availability (zero)
    // blocking consistency witht the shortest about of blocking time (least negative number)
    if (consistency != null) {
      return consistency.getBlockingTimestamp() - currentTime;
    } else {
      return 0L; // always return zero in availability mode
    }
  }
}
