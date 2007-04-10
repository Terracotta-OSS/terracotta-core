/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.exception.ImplementMe;

import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * This class provides a thread safe map interface (by extending Hashtable) and adds a way to easily and synchronously
 * iterator over the list of values as an array. This map is very useful when you want a snap shot of the values to
 * iterator over and dont want to hold up access to the map the whole time while you are iteratoring over the list to
 * avoid concurrent modification exception.
 * <p>
 * For example : <code>
 *  Hashtable t = ....
 *  for(Iterator i = t.values().iterator(); i.hashNext(); ) {
 *    // do something 
 *  }
 *  </code>
 * In the above code, if multiple threads are accessing t, to avoid ConcurrentModificationException, you need to
 * synchronize the entire for loop.
 * <p>
 * Using CopyOnWriteArrayMap and using the values() method will give you a snapshot of the values thus 
 * avoid synchronizing the map for the entire duration of the for loop.
 * <p>
 * This is achieved by maintaining an internal copy of the values in an array and copying that on modification. So an in
 * any CopyOnWrite class this is only effective on small datasets with lots of reads and few writes.
 */
public class CopyOnWriteArrayMap extends Hashtable {

  private volatile Object _values[] = new Object[0];

  public CopyOnWriteArrayMap() {
    super();
  }

  public CopyOnWriteArrayMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public CopyOnWriteArrayMap(int initialCapacity) {
    super(initialCapacity);
  }

  public CopyOnWriteArrayMap(Map t) {
    super(t);
  }

  public synchronized void clear() {
    super.clear();
    _values = new Object[0];
  }

  public Set entrySet() {
    throw new ImplementMe("This is not implemented yet due to lack to time."
        + " Support for remove needs to be provided by wrapping super iterators with my own iterators");
  }

  public Set keySet() {
    throw new ImplementMe("This is not implemented yet due to lack to time."
        + " Support for remove needs to be provided by wrapping super iterators with my own iterators");
  }

  public synchronized Object put(Object key, Object value) {
    Object old = super.put(key, value);
    if (old == null) {
      Object[] old_values = _values;
      _values = new Object[old_values.length + 1];
      System.arraycopy(old_values, 0, _values, 0, old_values.length);
      _values[old_values.length] = value;
    } else {
      Object[] old_values = _values;
      int length = old_values.length;
      // XXX:: doing an explicit copy so that the previous snapshots are not messed upon.
      _values = new Object[length];
      for (int i = 0; i < length; i++) {
        _values[i] = (old == old_values[i] ? value : old_values[i]);
      }
    }
    return old;
  }

  public synchronized void putAll(Map t) {
    // calls into put anyways
    super.putAll(t);
  }

  public synchronized Object remove(Object key) {
    Object old = super.remove(key);
    if (old != null) {
      Object[] old_values = _values;
      int length = old_values.length;
      _values = new Object[length - 1];
      int i = 0;
      boolean found = false;
      for (int j = 0; j < length; j++) {
        if (found || old != old_values[j]) {
          _values[i++] = old_values[j];
        } else {
          found = true;
        }
      }
    }
    return old;
  }

  public synchronized Collection values() {
    return Arrays.asList(_values);
  }
  
}
