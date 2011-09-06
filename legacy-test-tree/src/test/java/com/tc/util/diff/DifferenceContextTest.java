/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.diff;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.tc.test.TCTestCase;
import com.tc.util.StandardStringifier;
import com.tc.util.Stringifier;
import com.tc.util.TCAssertionError;
import com.tc.util.ToStringStringifier;

/**
 * Unit test for {@link DifferenceContext}.
 */
public class DifferenceContextTest extends TCTestCase {

  public void testInitial() throws Exception {
    assertEquals("", DifferenceContext.createInitial().toString());
    assertNotNull(DifferenceContext.createInitial().collection());
    assertFalse(DifferenceContext.createInitial().collection() == DifferenceContext.createInitial().collection());
    
    try {
      DifferenceContext.createInitial(null);
      fail("Didn't get NPE on no stringifier");
    } catch (NullPointerException npe) {
      // ok
    }
  }

  public void testStringifier() throws Exception {
    assertSame(StandardStringifier.INSTANCE, DifferenceContext.createInitial().stringifier());
    assertSame(ToStringStringifier.INSTANCE, DifferenceContext.createInitial(ToStringStringifier.INSTANCE)
        .stringifier());

    Stringifier s = new Stringifier() {
      public String toString(Object o) {
        return "X" + o + "Y";
      }
    };
    
    assertEquals("XaY", DifferenceContext.createInitial(s).describe("a"));
    assertEquals("XbY", DifferenceContext.createInitial(s).sub("Q").describe("b"));
  }

  public void testSubAndToString() throws Exception {
    DifferenceContext initial = DifferenceContext.createInitial();

    DifferenceContext a = initial.sub("a");
    assertNotSame(a, initial);
    assertEquals("/a", a.toString());
    assertEquals("", initial.toString());
    assertSame(initial.collection(), a.collection());

    DifferenceContext b = a.sub("b");
    assertNotSame(a, b);
    assertEquals("/a/b", b.toString());
    assertEquals("/a", a.toString());
    assertEquals("", initial.toString());
    assertSame(initial.collection(), b.collection());

    DifferenceContext c = b.sub("c");
    assertNotSame(c, b);
    assertNotSame(c, a);
    assertEquals("/a/b/c", c.toString());
    assertEquals("/a/b", b.toString());
    assertEquals("/a", a.toString());
    assertEquals("", initial.toString());
    assertSame(initial.collection(), c.collection());

    DifferenceContext d = a.sub("d");
    assertNotSame(d, c);
    assertNotSame(d, b);
    assertNotSame(d, a);
    assertEquals("/a/d", d.toString());
    assertEquals("/a/b/c", c.toString());
    assertEquals("/a/b", b.toString());
    assertEquals("/a", a.toString());
    assertEquals("", initial.toString());
    assertSame(initial.collection(), d.collection());
  }

  public void testDifferences() throws Exception {
    DifferenceContext initial = DifferenceContext.createInitial();
    DifferenceContext otherInitial = DifferenceContext.createInitial();

    assertNotSame(initial, otherInitial);

    assertEquals(initial, otherInitial);
    assertFalse(initial.hasDifferences());
    assertEqualsOrdered(new Object[0], initial.getDifferences());

    MockDifference initialDifference = new MockDifference(initial, "foo", "bar");
    initial.addDifference(initialDifference);
    assertFalse(initial.equals(otherInitial));
    assertTrue(initial.hasDifferences());
    assertFalse(otherInitial.hasDifferences());
    assertEqualsOrdered(new Object[] { initialDifference }, initial.getDifferences());
    assertEqualsOrdered(new Object[0], otherInitial.getDifferences());

    MockDifference otherInitialDifference = new MockDifference(otherInitial, "foo", "bar");
    otherInitial.addDifference(otherInitialDifference);
    assertEquals(initial, otherInitial);
    assertTrue(initial.hasDifferences());
    assertTrue(otherInitial.hasDifferences());
    assertEqualsOrdered(new Object[] { initialDifference }, initial.getDifferences());
    assertEqualsOrdered(new Object[] { otherInitialDifference }, otherInitial.getDifferences());

    initial.addDifference(new MockDifference(initial, "foo", "baz"));
    assertFalse(initial.equals(otherInitial));
    assertFalse(otherInitial.equals(initial));

    otherInitial.addDifference(new MockDifference(otherInitial, "foo", "quux"));
    assertFalse(initial.equals(otherInitial));
    assertFalse(otherInitial.equals(initial));

    initial = DifferenceContext.createInitial();
    otherInitial = DifferenceContext.createInitial();

    assertEquals(initial, otherInitial);
    assertNotSame(initial, otherInitial);

    DifferenceContext initA = initial.sub("a");
    DifferenceContext otherInitA = otherInitial.sub("a");
    DifferenceContext initB = initA.sub("b");
    DifferenceContext otherInitB = otherInitA.sub("b");
    DifferenceContext initC = initial.sub("c");
    DifferenceContext otherInitC = otherInitial.sub("c");

    assertEquals(initial, otherInitial);
    assertEquals(initA, otherInitA);
    assertEquals(initB, otherInitB);
    assertEquals(initC, otherInitC);

    MockDifference diffInit = new MockDifference(initial, "1", "2");
    MockDifference diffOtherInit = new MockDifference(otherInitial, "1", "2");
    MockDifference diffInitA = new MockDifference(initA, "3", "4");
    MockDifference diffOtherInitA = new MockDifference(otherInitA, "3", "4");
    MockDifference diffInitB = new MockDifference(initB, "5", "6");
    MockDifference diffOtherInitB = new MockDifference(otherInitB, "5", "6");
    MockDifference diffInitC = new MockDifference(initC, "7", "8");
    MockDifference diffOtherInitC = new MockDifference(otherInitC, "7", "8");

    DifferenceContext[] initialContexts = new DifferenceContext[] { initial, initA, initB, initC };
    DifferenceContext[] otherInitialContexts = new DifferenceContext[] { otherInitial, otherInitA, otherInitB,
        otherInitC };

    initial.addDifference(diffInit);
    checkDifferences(initialContexts, new Object[] { diffInit }, otherInitialContexts, new Object[0]);
    otherInitial.addDifference(diffOtherInit);
    checkDifferences(initialContexts, new Object[] { diffInit }, otherInitialContexts, new Object[] { diffOtherInit });

    initA.addDifference(diffInitA);
    checkDifferences(initialContexts, new Object[] { diffInit, diffInitA }, otherInitialContexts,
                     new Object[] { diffOtherInit });
    otherInitA.addDifference(diffOtherInitA);
    checkDifferences(initialContexts, new Object[] { diffInit, diffInitA }, otherInitialContexts, new Object[] {
        diffOtherInit, diffOtherInitA });

    initB.addDifference(diffInitB);
    checkDifferences(initialContexts, new Object[] { diffInit, diffInitA, diffInitB }, otherInitialContexts,
                     new Object[] { diffOtherInit, diffOtherInitA });
    otherInitB.addDifference(diffOtherInitB);
    checkDifferences(initialContexts, new Object[] { diffInit, diffInitA, diffInitB }, otherInitialContexts,
                     new Object[] { diffOtherInit, diffOtherInitA, diffOtherInitB });

    initC.addDifference(diffInitC);
    checkDifferences(initialContexts, new Object[] { diffInit, diffInitA, diffInitB, diffInitC }, otherInitialContexts,
                     new Object[] { diffOtherInit, diffOtherInitA, diffOtherInitB });
    otherInitC.addDifference(diffOtherInitC);
    checkDifferences(initialContexts, new Object[] { diffInit, diffInitA, diffInitB, diffInitC }, otherInitialContexts,
                     new Object[] { diffOtherInit, diffOtherInitA, diffOtherInitB, diffOtherInitC });

    try {
      initial.addDifference(null);
      fail("Didn't get NPE on adding null difference");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      initial.addDifference(otherInitialDifference);
      fail("Didn't get TCAE on add of difference with different context");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      initial.addDifference(new MockDifference(initial.sub("a")));
      fail("Didn't get TCAE on add of difference with different context");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      initial.sub("a").addDifference(otherInitialDifference);
      fail("Didn't get TCAE on add of difference with different context");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

  private void checkDifferences(DifferenceContext[] oneContexts, Object[] expectedOneDifferences,
                                DifferenceContext[] twoContexts, Object[] expectedTwoDifferences) {
    for (int i = 0; i < oneContexts.length; ++i) {
      assertEqualsOrdered(expectedOneDifferences, oneContexts[i].getDifferences());
      if (expectedOneDifferences.length > 0) assertTrue(oneContexts[i].hasDifferences());
      else assertFalse(oneContexts[i].hasDifferences());
      assertEquals(expectedOneDifferences.length, oneContexts[i].countDifferences());
    }

    for (int i = 0; i < twoContexts.length; ++i) {
      assertEqualsOrdered(expectedTwoDifferences, twoContexts[i].getDifferences());
      if (expectedTwoDifferences.length > 0) assertTrue(twoContexts[i].hasDifferences());
      else assertFalse(twoContexts[i].hasDifferences());
      assertEquals(expectedTwoDifferences.length, twoContexts[i].countDifferences());
    }

    boolean shouldBeEqual = new EqualsBuilder().append(expectedOneDifferences, expectedTwoDifferences).isEquals();
    for (int i = 0; i < oneContexts.length; ++i) {
      for (int j = 0; j < twoContexts.length; ++j) {
        if (shouldBeEqual) {
          assertEquals(oneContexts[i], twoContexts[i]);
          assertEquals(twoContexts[i], oneContexts[i]);
        } else {
          assertFalse(oneContexts[i].equals(twoContexts[i]));
          assertFalse(twoContexts[i].equals(oneContexts[i]));
        }
      }
    }
  }

  public void testEquals() throws Exception {
    DifferenceContext initial = DifferenceContext.createInitial();
    DifferenceContext otherInitial = DifferenceContext.createInitial();

    assertEquals(initial, otherInitial);
    assertEquals(otherInitial, initial);

    assertEquals(initial.sub("a"), otherInitial.sub("a"));
    assertEquals(initial.sub("a").sub("b"), otherInitial.sub("a").sub("b"));

    assertEquals(initial, initial);
    assertFalse(initial.equals(initial.sub("a")));
    assertFalse(initial.sub("a").equals(initial));

    assertEquals(initial.sub("a"), initial.sub("a"));
    assertFalse(initial.sub("a").equals(initial.sub("b")));
    assertFalse(initial.sub("a").equals(initial.sub("a").sub("b")));

    assertEquals(initial.sub("a").sub("b"), initial.sub("a").sub("b"));
    assertFalse(initial.sub("a").sub("b").equals(initial.sub("a")));
    assertFalse(initial.sub("a").sub("b").equals(initial.sub("a").sub("c")));
    assertFalse(initial.sub("a").sub("b").equals(initial.sub("b").sub("a")));
  }

}