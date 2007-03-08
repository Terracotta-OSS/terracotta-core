/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.TraversedReference;
import com.tc.object.appevent.NonPortableEventContext;

import java.util.Collections;
import java.util.Set;

/**
 * Registry for non-distributable objects Loaded from boot jar by native classloader, singleton across the VM.
 */
public class NonDistributableObjectRegistry /* implements TraverseTest */{

  {
    // XXX: Most of the support for this class has been cleaned up. To wire this class back up,
    // we need add back a way to let the DSO client use this a TraverseTest when walking the graph of
    // newly shared objects. Additionally, you want the Set used internally to based on an IdentityWeakHashSet, not just
    // a regular HashSet
    if (true) throw new UnsupportedOperationException();
  }

  private static final NonDistributableObjectRegistry instance          = new NonDistributableObjectRegistry();

  private Set                                         nonDistributables = null;
  private boolean                                     added             = false;

  public boolean isAdded() {
    return added;
  }

  public void setAdded() {
    added = true;
  }

  private NonDistributableObjectRegistry() {
    nonDistributables = Collections.unmodifiableSet(Collections.EMPTY_SET);

    // potential perfomance bottleneck
    // nonDistributables = Collections.synchronizedSet(new IdentityWeakHashSet());
  }

  public static NonDistributableObjectRegistry getInstance() {
    return instance;
  }

  public boolean shouldTraverse(Object obj) {
    return obj != null && nonDistributables.contains(obj) == false;
  }

  public Set getNondistributables() {
    return nonDistributables;
  }

  public void checkPortability(TraversedReference obj, Class referringClass, NonPortableEventContext context) {
    // never fails
  }

}
