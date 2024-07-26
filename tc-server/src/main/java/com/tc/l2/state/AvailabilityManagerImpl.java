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
package com.tc.l2.state;

import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.net.NodeID;
import com.tc.objectserver.impl.Topology;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AvailabilityManagerImpl implements ConsistencyManager {
  
  public AvailabilityManagerImpl() {
  }
  
  @Override
  public boolean requestTransition(ServerMode mode, NodeID sourceNode, Topology topology, Transition newMode) throws IllegalStateException {
    return true;
  }

  @Override
  public boolean lastTransitionSuspended() {
    return false;
  }

  @Override
  public void allowLastTransition() {
    //
  }

  @Override
  public Collection<Transition> requestedActions() {
    return Collections.emptySet();
  }

  @Override
  public long getCurrentTerm() {
    return 0L;
  }

  @Override
  public void setCurrentTerm(long term) {
  }

  @Override
  public Enrollment createVerificationEnrollment(NodeID lastActive, WeightGeneratorFactory weightFactory) {
    Enrollment e = EnrollmentFactory.createTrumpEnrollment(lastActive, weightFactory);
    return e;
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", "Availability");
    return map;
  }
}
