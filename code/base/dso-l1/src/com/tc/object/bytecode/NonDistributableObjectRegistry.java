/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.TraverseTest;
import com.tc.object.TraversedReference;
import com.tc.object.appevent.NonPortableEventContext;
import com.tc.object.util.IdentityWeakHashSet;

import java.util.Collections;
import java.util.Set;

/**
 * Registry for non-distributable objects
 * 
 * Loaded from boot jar by native classloader, singleton across the VM.
 */
public class NonDistributableObjectRegistry implements TraverseTest {

  private static final NonDistributableObjectRegistry instance = new NonDistributableObjectRegistry();
  
  private Set nonDistributables = null;
  private boolean added = false;
  
  public boolean isAdded() {
    return added;
  }
  
  public void setAdded() {
    added = true;
  }
  
  
  private NonDistributableObjectRegistry() {
    // potential perfomance bottleneck
    nonDistributables = Collections.synchronizedSet(new IdentityWeakHashSet());
  }
  
  public static NonDistributableObjectRegistry getInstance() {
    return instance;
  }

  public boolean shouldTraverse(Object obj) {
    return obj != null && nonDistributables.contains(obj)==false;
  }
 
  public Set getNondistributables() {
    return nonDistributables;
  }

  public void checkPortability(TraversedReference obj, Class referringClass, NonPortableEventContext context) {
    // never fails
  }
  
}

