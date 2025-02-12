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

public class EnrollmentFactory {

  public static Enrollment createEnrollment(NodeID nodeID, boolean isNew, WeightGeneratorFactory weightFactory) {
    long[] weights =  weightFactory.generateWeightSequence();
    Enrollment e = new Enrollment(nodeID, isNew, weights);
    return e;
  }
  
  public static Enrollment createTrumpEnrollment(NodeID myNodeId, WeightGeneratorFactory weightFactory) {
    long[] weights = weightFactory.generateMaxWeightSequence();
    Enrollment e = new Enrollment(myNodeId, false, weights);
    return e;
  }
  
  public static Enrollment createVerificationEnrollment(NodeID lastActive, WeightGeneratorFactory weightFactory) {
    long[] weights = weightFactory.generateVerificationSequence();
    Enrollment e = new Enrollment(lastActive, false, weights);
    return e;
  }
}
