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
package com.tc.l2.state;

import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.net.NodeID;
import com.tc.net.utils.L2Utils;
import com.tc.objectserver.impl.Topology;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class DiagnosticModeConsistencyManager implements ConsistencyManager {
  @Override
  public boolean requestTransition(ServerMode mode, NodeID sourceNode, Topology topology, Transition newMode) throws IllegalStateException {
    // to avoid further elections and transition requests
    waitForEver();

    return false;
  }

  @Override
  public boolean lastTransitionSuspended() {
    return false;
  }

  @Override
  public void allowLastTransition() {

  }

  @Override
  public Collection<Transition> requestedActions() {
    return null;
  }

  @Override
  public long getCurrentTerm() {
    return 0;
  }

  @Override
  public void setCurrentTerm(long term) {

  }

  private static void waitForEver() {
    try {
      new CountDownLatch(1).await();
    } catch (InterruptedException e) {
          L2Utils.handleInterrupted(null, e);
    }
  }

  @Override
  public Enrollment createVerificationEnrollment(NodeID lastActive, WeightGeneratorFactory weightFactory) {
    return EnrollmentFactory.createTrumpEnrollment(lastActive, weightFactory);
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", "Diagnostic");
    return map;
  }
  
  
}
