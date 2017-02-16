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
