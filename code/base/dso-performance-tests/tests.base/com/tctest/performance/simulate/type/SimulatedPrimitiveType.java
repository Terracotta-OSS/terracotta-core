/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.simulate.type;

import java.lang.reflect.Constructor;

final class SimulatedPrimitiveType extends AbstractSimulatedType {

  private Object     masterCopy;
  private static int value;
  private static int iteration = 1;

  SimulatedPrimitiveType(Object obj) {
    this.masterCopy = obj;
  }

  public Class getType() {
    return masterCopy.getClass();
  }

  public Object cloneUnique() {
    try {
      Constructor constructor = masterCopy.getClass().getConstructor(new Class[] { String.class });
      return constructor.newInstance(new Object[] { String.valueOf(incrementValue()) });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Object clone() {
    try {
      Constructor constructor = masterCopy.getClass().getConstructor(new Class[] { String.class });
      return constructor.newInstance(new Object[] { masterCopy.toString() });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private static synchronized int value() {
    return value;
  }

  private static synchronized int iteration() {
    return iteration;
  }

  private static synchronized int incrementValue() {
    if (value == Integer.MAX_VALUE) {
      value = 0;
      incrementIteration();
    }
    return ++value;
  }

  private static synchronized void incrementIteration() {
    iteration++;
  }

  public String toString() {
    return "{type=" + masterCopy.getClass().getName() + " unique-value=" + value() + " iteration=" + iteration()
           + " masterCopy=" + masterCopy + "}";
  }
}
