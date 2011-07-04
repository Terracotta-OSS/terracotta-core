/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.diff;

import com.tc.test.TCTestCase;

/**
 * Unit test for {@link PrimitiveDifference}.
 */
public class PrimitiveDifferenceTest extends TCTestCase {

  public void testConstructionAandBandToString() throws Exception {
    PrimitiveDifference diff;
    DifferenceContext ctx = DifferenceContext.createInitial().sub("a").sub("q");

    diff = new PrimitiveDifference(ctx, true, false);
    assertEquals(Boolean.TRUE, diff.a());
    assertEquals(Boolean.FALSE, diff.b());
    assertTrue(diff.toString().indexOf("true") >= 0);
    assertTrue(diff.toString().indexOf("false") >= 0);

    diff = new PrimitiveDifference(ctx, (byte) 14, (byte) 37);
    assertEquals(new Byte((byte) 14), diff.a());
    assertEquals(new Byte((byte) 37), diff.b());
    assertTrue(diff.toString().indexOf("14") >= 0);
    assertTrue(diff.toString().indexOf("37") >= 0);

    diff = new PrimitiveDifference(ctx, 'a', 'q');
    assertEquals(new Character('a'), diff.a());
    assertEquals(new Character('q'), diff.b());
    assertTrue(diff.toString().indexOf("a") >= 0);
    assertTrue(diff.toString().indexOf("q") >= 0);

    diff = new PrimitiveDifference(ctx, (short) 14, (short) 37);
    assertEquals(new Short((short) 14), diff.a());
    assertEquals(new Short((short) 37), diff.b());
    assertTrue(diff.toString().indexOf("14") >= 0);
    assertTrue(diff.toString().indexOf("37") >= 0);

    diff = new PrimitiveDifference(ctx, 14, 37);
    assertEquals(new Integer(14), diff.a());
    assertEquals(new Integer(37), diff.b());
    assertTrue(diff.toString().indexOf("14") >= 0);
    assertTrue(diff.toString().indexOf("37") >= 0);

    diff = new PrimitiveDifference(ctx, (long) 14, (long) 37);
    assertEquals(new Long(14), diff.a());
    assertEquals(new Long(37), diff.b());
    assertTrue(diff.toString().indexOf("14") >= 0);
    assertTrue(diff.toString().indexOf("37") >= 0);

    diff = new PrimitiveDifference(ctx, 14.0f, 37.0f);
    assertEquals(new Float(14.0f), diff.a());
    assertEquals(new Float(37.0f), diff.b());
    assertTrue(diff.toString().indexOf("14.0") >= 0);
    assertTrue(diff.toString().indexOf("37.0") >= 0);

    diff = new PrimitiveDifference(ctx, 14.0, 37.0);
    assertEquals(new Double(14.0), diff.a());
    assertEquals(new Double(37.0), diff.b());
    assertTrue(diff.toString().indexOf("14.0") >= 0);
    assertTrue(diff.toString().indexOf("37.0") >= 0);
  }

  public void testEquals() throws Exception {
    assertEquals(new PrimitiveDifference(DifferenceContext.createInitial().sub("foo"), 14, 37),
                 new PrimitiveDifference(DifferenceContext.createInitial().sub("foo"), 14, 37));
    assertFalse(new PrimitiveDifference(DifferenceContext.createInitial().sub("foo"), 14, 37)
        .equals(new PrimitiveDifference(DifferenceContext.createInitial().sub("bar"), 14, 37)));
    assertFalse(new PrimitiveDifference(DifferenceContext.createInitial().sub("foo"), 14, 37)
        .equals(new PrimitiveDifference(DifferenceContext.createInitial().sub("foo"), 15, 37)));
    assertFalse(new PrimitiveDifference(DifferenceContext.createInitial().sub("foo"), 14, 37)
        .equals(new PrimitiveDifference(DifferenceContext.createInitial().sub("foo"), 14, 38)));
    assertFalse(new PrimitiveDifference(DifferenceContext.createInitial().sub("foo"), 14, 37).equals("foo"));
    assertFalse(new PrimitiveDifference(DifferenceContext.createInitial().sub("foo"), 14, 37).equals(null));
  }

}