/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class ComplexBeanIdTest extends TestCase {
  private ComplexBeanId id1 = null;
  private ComplexBeanId id2 = null;
  private ComplexBeanId id3 = null;
  private ComplexBeanId id4 = null;
  

  protected void setUp() throws Exception {
    super.setUp();
    id1 = new ComplexBeanId("scope1", "bean", true);
    id2 = new ComplexBeanId("scope1", "bean", false);

    id3 = new ComplexBeanId("5C2180007C5B82736601!1(2)", "scopedTarget.simplebean", true);
    id4 = new ComplexBeanId("5C2180007C5B82736601!1(2)", "scopedTarget.simplebean", false);
  }
  
  public void testEquals() throws Exception {
    Map m = new HashMap();
    
    m.put(id2, "val1");
    m.put(id4, "val2");
    
    Object val = m.get(id1);

    assertEquals("val1", val);
    assertNotNull(id1.getEqualPeer());
    assertNotNull(id2.getEqualPeer());
    assertSame(id1.getEqualPeer(), id2);
    assertSame(id2.getEqualPeer(), id1);
    
    val = m.get(id3);

    assertEquals("val2", val);
    assertNotNull(id3.getEqualPeer());
    assertNotNull(id4.getEqualPeer());
    assertSame(id3.getEqualPeer(), id4);
    assertSame(id4.getEqualPeer(), id3);
    
  }

}
