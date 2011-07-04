/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.diff;

import com.tc.exception.ImplementMe;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link Difference}.
 */
public class DifferenceTest extends TCTestCase {

  // This just is to get around the fact that Difference is abstract.
  private static class TestDifference extends Difference {
    public TestDifference(DifferenceContext where) {
      super(where);
    }

    public Object a() {
      throw new ImplementMe();
    }

    public Object b() {
      throw new ImplementMe();
    }

    public String toString() {
      throw new ImplementMe();
    }
  }

  public void testConstruction() throws Exception {
    try {
      new TestDifference(null);
      fail("Didn't get NPE on no context");
    } catch (NullPointerException npe) {
      // ok
    }
  }
  
  public void testWhere() throws Exception {
    DifferenceContext context = DifferenceContext.createInitial().sub("a").sub("b");
    Difference test = new TestDifference(context);
    
    assertSame(context, test.where());
  }
  
  public void testEquals() throws Exception {
    DifferenceContext contextA = DifferenceContext.createInitial().sub("a");
    DifferenceContext contextB = DifferenceContext.createInitial().sub("b");
    
    Difference a = new TestDifference(contextA);
    Difference b = new TestDifference(contextB);
    Difference c = new TestDifference(contextA);
    
    assertEquals(a, c);
    assertEquals(c, a);
    
    assertFalse(a.equals(b));
    assertFalse(b.equals(a));
    assertFalse(c.equals(b));
    assertFalse(b.equals(c));
  }

}