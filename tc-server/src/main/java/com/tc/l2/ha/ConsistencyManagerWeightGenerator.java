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
package com.tc.l2.ha;

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateManager;
import java.util.function.Supplier;


public class ConsistencyManagerWeightGenerator implements WeightGenerator {
  private final boolean isAvailable;
  private final Supplier<StateManager> state;

  public ConsistencyManagerWeightGenerator(Supplier<StateManager> state, boolean isAvailable) {
    this.state = state;
    this.isAvailable = isAvailable;
  }
  /**
   * this weight is now obsolete, keep it for compatibility, will use it to designate the active
   * in availability mode.  In consistency mode, the active is not relevant.  This weighting is 
   * only used during a verification election as actives do not participate in bootstrap elections.
   * 
   * @return 
   */
  @Override
  public long getWeight() {
    long weight = 0L;
    if (state.get().getCurrentMode() == ServerMode.RELAY) {
      weight = -1L;
    } else if (state.get().isActiveCoordinator()) {
      weight |= 0x08;
    } else if (state.get().getCurrentMode() == ServerMode.PASSIVE) {
      weight |= 0x01;
    }

    return weight;
  }

  @Override
  public boolean isVerificationWeight() {
    // only participate in election verification if in availability mode.  
    return false;
  }
}
