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
import com.tc.l2.state.StateManager;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.util.Assert;

public class InitialStateWeightGenerator implements WeightGenerator {
  private final ClusterStatePersistor state;

  public InitialStateWeightGenerator(ClusterStatePersistor state) {
    Assert.assertNotNull(state);
    this.state = state;
  }

  @Override
  public long getWeight() {
    // active initially should win
    return StateManager.ACTIVE_COORDINATOR.equals(state.getInitialState()) ? 1 : 0;
  }

}
