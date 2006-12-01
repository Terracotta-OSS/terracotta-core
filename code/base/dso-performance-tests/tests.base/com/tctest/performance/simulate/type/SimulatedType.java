/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.simulate.type;

public interface SimulatedType {

  Class getType();

  Object cloneUnique();

  Object clone();
  
  Object clone(int percentUnique);
}
