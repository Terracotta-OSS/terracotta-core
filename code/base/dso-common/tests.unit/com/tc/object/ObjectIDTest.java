/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class ObjectIDTest extends TestCase {
  
  public void testIdentity() {
    int idValue = 1001;
    ObjectID id = new ObjectID(idValue);
    ObjectID clone = new ObjectID(idValue);

    assertNotSame(id, clone);
    assertEquals(id, clone);
    
    Set set = new HashSet();
    set.add(clone);
    
    assertTrue(set.contains(id));
    set.remove(id);
    assertEquals(0, set.size());
    
    
    Map map = new HashMap();
    Object o = new Object();
    map.put(id, o);
    assertEquals(o, map.get(clone));
  }
}
