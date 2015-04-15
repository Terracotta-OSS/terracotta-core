/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

}
