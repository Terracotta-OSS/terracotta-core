/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.simulate.type;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class SimulatedMap extends AbstractSimulatedType {

  private Map masterCopy;

  /**
   * @param Map - key and value entries must be of type: {@link SimulatedType}
   */
  SimulatedMap(Map map) {
    Set entries = map.entrySet();
    Map.Entry entry;
    for (Iterator iter = entries.iterator(); iter.hasNext();) {
      entry = (Map.Entry) iter.next();
      if (!(entry.getKey() instanceof SimulatedType)) throw new RuntimeException("Keys must be of type: "
                                                                                 + SimulatedType.class.getName());
      if (!(entry.getValue() instanceof SimulatedType)) throw new RuntimeException("Values must be of type: "
                                                                                   + SimulatedType.class.getName());
    }
    this.masterCopy = map;
  }

  public Class getType() {
    return masterCopy.getClass();
  }

  public Object cloneUnique() {
    try {
      Constructor constructor = masterCopy.getClass().getConstructor(new Class[0]);
      Map clone = (Map) constructor.newInstance(new Object[0]);
      Set entries = masterCopy.entrySet();
      Map.Entry entry;
      Object keyClone;
      Object valueClone;
      for (Iterator iter = entries.iterator(); iter.hasNext();) {
        entry = (Map.Entry) iter.next();
        keyClone = ((SimulatedType) entry.getKey()).cloneUnique();
        valueClone = ((SimulatedType) entry.getValue()).cloneUnique();
        clone.put(keyClone, valueClone);
      }
      return clone;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Object clone() {
    try {
      Constructor constructor = masterCopy.getClass().getConstructor(new Class[0]);
      Map clone = (Map) constructor.newInstance(new Object[0]);
      Set entries = masterCopy.entrySet();
      Map.Entry entry;
      Object keyClone;
      Object valueClone;
      for (Iterator iter = entries.iterator(); iter.hasNext();) {
        entry = (Map.Entry) iter.next();
        keyClone = ((SimulatedType) entry.getKey()).clone();
        valueClone = ((SimulatedType) entry.getValue()).clone();
        clone.put(keyClone, valueClone);
      }
      return clone;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String toString() {
    return "{type=" + masterCopy.getClass().getName() + " keys=" + Arrays.asList(masterCopy.entrySet().toArray()) + "}";
  }
}
