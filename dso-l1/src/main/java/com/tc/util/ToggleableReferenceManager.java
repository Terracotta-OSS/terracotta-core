/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceIdentityMap;

import com.tc.object.util.ToggleableStrongReference;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * This class provides a factory/manager for ToggleableHardReference instances. In some scenarios, we want an easy way
 * to programatically create strong references to shared objects. The one motivation for this is that strongly
 * referenced shared objects (referred from non-shared) will never be flushed by the memory manager. <br>
 * <br>
 * Internally this class maintains a map from pojos to toggleable references. The references in the map always weakly
 * references the peer pojo, and can toggle between having a strong reference or not. Since the internal map is not a
 * shared object and it strongly references the reference object (which can in sometimes strongly references the pojo),
 * one can ensure that a pojo is strongly reachable.<br>
 * <br>
 * The correct use of toggleable references is from within a method of the target pojo (since you can be guaranteed that
 * "this" is strongly reachable at the time the reference is created/toggled)
 */
public class ToggleableReferenceManager {

  private final Map<Object, ToggleableStrongReference> refs = new ReferenceIdentityMap(AbstractReferenceMap.WEAK,
                                                                                       AbstractReferenceMap.HARD, true);

  public ToggleableReferenceManager() {
    //
  }

  public ToggleableStrongReference getOrCreateFor(Object peer) {
    if (peer == null) { throw new NullPointerException("null peer object"); }

    ToggleableStrongReference rv;

    synchronized (refs) {
      rv = refs.get(peer);
      if (rv == null) {
        rv = new SometimesStrongAlwaysWeakReference(peer);
        refs.put(peer, rv);
      }
    }
    return rv;
  }

  // for tests
  int size() {
    synchronized (refs) {
      return refs.size();
    }
  }

  private static class SometimesStrongAlwaysWeakReference extends WeakReference implements ToggleableStrongReference {
    private volatile Object strongReference;

    public SometimesStrongAlwaysWeakReference(Object peer) {
      super(peer);
    }

    public void strongRef(Object obj) {
      if (obj == null) { throw new NullPointerException(); }
      this.strongReference = obj;
    }

    public void clearStrongRef() {
      this.strongReference = null;
    }

    @Override
    public String toString() {
      Object o = strongReference;
      return getClass().getName() + o == null ? "" : " (strongRef to " + o.getClass().getName() + ")";
    }
  }

}
