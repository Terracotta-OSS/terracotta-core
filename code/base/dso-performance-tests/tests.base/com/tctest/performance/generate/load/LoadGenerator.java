/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.generate.load;

import com.tctest.performance.simulate.type.SimulatedType;

public interface LoadGenerator {

  /**
   * @param durration - in seconds
   * @param minLoad - beginning number of objects per second
   * @param maxLoad - final number of objects per second
   * @param factory - used to clone new objects
   * @param percentUnique - 0 to 100 defines uniqueness of cloned object values
   */
  void start(int duration, int minLoad, int maxLoad, SimulatedType factory, int percentUnique);

  /**
   * @return - null to indicate completion
   */
  Object getNext() throws InterruptedException, WorkQueueOverflowException;

  Measurement[] getWaitTimes();
}
