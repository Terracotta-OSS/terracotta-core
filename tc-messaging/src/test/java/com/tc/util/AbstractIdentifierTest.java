/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util;

import org.junit.Test;

/**
 * Unit test for {@link AbstractIdentifier}.
 */
public class AbstractIdentifierTest {

  @Test
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
  
  @Test
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
    @Override
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
    @Override
    public String getIdentifierType() {
      return "BlueFishID";
    }
  }

  
  
}
