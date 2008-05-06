/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class GlobalKeySetTest extends TestCase {
  
  private GlobalKeySet gks;
  
  public void setUp() {
    gks = new GlobalKeySet();
  }
  
  public void testTypicalGlobalEvictionCycle() {
    // Node 1 is global evictor and starts global eviction
    assertFalse(gks.inGlobalEviction());
    gks.globalEvictionStart(new Object[] { "k1" });
    
    // Node 2 does a local eviction
    assertTrue(gks.inGlobalEviction());
    gks.addLocalKeySet(new Object[] { "k1", "k2" });

    // Node 1 does another local eviction during global cycle
    assertTrue(gks.inGlobalEviction());

    // Node 3 does a local eviction
    assertTrue(gks.inGlobalEviction());
    // note keys are not mutually exclusive and can be managed on multiple nodes like "k2" here
    gks.addLocalKeySet(new Object[] { "k2", "k3", "k4" });  
    
    // Node 1 checks for end of global eviction and it's finally time
    assertTrue(gks.inGlobalEviction());
    Collection remoteKeys = gks.globalEvictionEnd();
    assertEquals(toSet(new String[] { "k1", "k2", "k3", "k4" }), new HashSet(remoteKeys));
  }
  
  public void testAllNodesDieDuringGlobalEviction() {
    // Node 1 is global evictor and starts global eviction
    assertFalse(gks.inGlobalEviction());
    gks.globalEvictionStart(new Object[] { "k1" });
    
    // Node 2 does a local eviction
    assertTrue(gks.inGlobalEviction());
    gks.addLocalKeySet(new Object[] { "k1", "k2" });

    // Node 2 comes back and will become the new global evictor
    
    // Node 2 hits global eviction frequency (its still set as running from before)
    assertTrue(gks.inGlobalEviction());
    Collection remoteKeys = gks.globalEvictionEnd();
    assertEquals(toSet(new String[] { "k1", "k2" }), new HashSet(remoteKeys));
    
    // Here the orphaned keys from nodes that had not yet reported will
    // be detected and handled.  Keys from nodes that had reported will be
    // seen (possibly incorrectly as orphans).  This is going to cause 
    // faulting, but this is an unusual (and presumably rare case).  
  }

  private Set toSet(String[] values) {
    Set s = new HashSet();
    if(values != null) {
      for(int i=0; i<values.length; i++) {
        s.add(values[i]);
      }
    }
    return s;
  }
  
}
