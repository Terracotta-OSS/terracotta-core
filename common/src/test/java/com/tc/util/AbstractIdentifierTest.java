/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

/**
 * Unit test for {@link AbstractIdentifier}.
 */
public class AbstractIdentifierTest extends TCTestCase {

  public void testNonNullID() {
    RedFishID r = new RedFishID(100);
    Assert.assertEquals("RedFish", r.getIdentifierType());
    Assert.assertEquals(false, r.isNull());
    Assert.assertEquals(100, r.toLong());
    
    // Check equality with self
    Assert.assertEquals(r, r);
    
    // Check equality with "same" but not identical
    RedFishID s = new RedFishID(100);
    Assert.assertTrue(r.equals(s));
    
    // Check inequality with same value, different class
    BlueFishID t = new BlueFishID(100);
    Assert.assertTrue(!r.equals(t));
  }
  
  public void testNullID() {
    RedFishID r = new RedFishID();
    Assert.assertEquals("RedFish", r.getIdentifierType());
    Assert.assertEquals(true, r.isNull());
    Assert.assertEquals(-1, r.toLong());
    
    // Check equality with self
    Assert.assertEquals(r, r);
    
    // Check equality with "same" but not identical
    RedFishID s = new RedFishID(-1);
    Assert.assertTrue(r.equals(s));
    
    // Check inequality with same value, different class
    BlueFishID t = new BlueFishID();
    Assert.assertTrue(!r.equals(t));
  }
  
  private static class RedFishID extends AbstractIdentifier {
    public RedFishID() {
      super();
    }
    public RedFishID(long id) {
      super(id);
    }
    public String getIdentifierType() {
      return "RedFish";
    }
  }
  
  private static class BlueFishID extends AbstractIdentifier {
    public BlueFishID() {
      super();
    }
    public BlueFishID(long id) {
      super(id);
    }
    public String getIdentifierType() {
      return "BlueFishID";
    }
  }

  
  
}
