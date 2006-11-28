/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.simulate.type;

public interface SimulatedType {

  Class getType();

  Object cloneUnique();

  Object clone();
  
  Object clone(int percentUnique);
}
