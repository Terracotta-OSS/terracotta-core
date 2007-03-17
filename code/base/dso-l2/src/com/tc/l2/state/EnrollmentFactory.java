/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.state;

import com.tc.net.groups.NodeID;

import java.util.Random;

public class EnrollmentFactory {

  public static Enrollment createEnrollment(NodeID nodeID, boolean isNew) {
    int[] weights = createWeigth();
    Enrollment e = new Enrollment(nodeID, isNew, weights);
    return e;
  }

  //TODO:: Create weights based on hardware config, ip address, port number, nodeID, process id etc
  // FIXME:: This is a temperary implementation
  private static int[] createWeigth() {
    int[] weights = new int[2];
    Random r = new Random(); 
    weights[1] = r.nextInt();
    weights[0] = r.nextInt();
    return weights;
  }
  
  // FIXME:: This is a temperary implementation
  private static int[] createMaxWeigth() {
    int[] weights = new int[2];
    weights[1] = Integer.MAX_VALUE;
    weights[0] = Integer.MAX_VALUE;
    return weights;
  }


  public static Enrollment createTrumpEnrollment(NodeID myNodeId) {
    int[] weights = createMaxWeigth();
    Enrollment e = new Enrollment(myNodeId, false, weights);
    return e;
  }

}
