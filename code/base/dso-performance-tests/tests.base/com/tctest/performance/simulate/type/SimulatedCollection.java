/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.simulate.type;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

final class SimulatedCollection extends AbstractSimulatedType {

  private Collection masterCopy;

  /**
   * @param collection - elements must be of type: {@link SimulatedType}
   */
  SimulatedCollection(Collection collection) {
    for (Iterator iter = collection.iterator(); iter.hasNext();) {
      if (!(iter.next() instanceof SimulatedType)) throw new RuntimeException("Collection elements must be of type: "
                                                                              + SimulatedType.class.getName());
    }
    this.masterCopy = collection;
  }

  public Class getType() {
    return masterCopy.getClass();
  }

  public Object cloneUnique() {
    try {
      Constructor constructor = masterCopy.getClass().getConstructor(new Class[0]);
      Collection clone = (Collection) constructor.newInstance(new Object[0]);
      for (Iterator iter = masterCopy.iterator(); iter.hasNext();) {
        clone.add(((SimulatedType) iter.next()).cloneUnique());
      }
      return clone;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public Object clone() {
    try {
      Constructor constructor = masterCopy.getClass().getConstructor(new Class[0]);
      Collection clone = (Collection) constructor.newInstance(new Object[0]);
      for (Iterator iter = masterCopy.iterator(); iter.hasNext();) {
        clone.add(((SimulatedType) iter.next()).clone());
      }
      return clone;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String toString() {
    return "{type=" + masterCopy.getClass().getName() + " elements=" + Arrays.asList(masterCopy.toArray()) + "}";
  }
}
