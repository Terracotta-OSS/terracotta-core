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
package com.tc.l2.state;

import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.net.NodeID;
import com.tc.objectserver.impl.Topology;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AvailabilityManagerImpl implements ConsistencyManager {
  
  private final boolean compatibility;

  public AvailabilityManagerImpl(boolean uc) {
    compatibility = uc;
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
    if (compatibility) {
      // if in compatibility mode, make the term generator zero to be compatible with old versions
      e.getWeights()[e.getWeights().length-1] = 0;
    }
    return e;
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", "Availability");
    return map;
  }
}
