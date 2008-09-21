/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
