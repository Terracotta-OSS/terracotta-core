/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.collections;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.tc.util.diff.DifferenceBuilder;
import com.tc.util.diff.DifferenceContext;
import com.tc.util.diff.Differenceable;

/**
 * Unit test for {@link OrderedCollectionComparer}.
 */
public class OrderedCollectionComparerTest extends CollectionComparerTestBase {

  public void setUp() throws Exception {
    super.setUp();
    this.comparer = new OrderedCollectionComparer(this.equalityComparator, this.describer);
  }

  public void testChecksOrder() throws Exception {
    MyObj one = new MyObj("a");
    MyObj two = new MyObj("b");

    checkMismatches(new CollectionMismatch[] {
        new UnequalObjectCollectionMismatch(one, two, true, 0, 0, this.describer),
        new UnequalObjectCollectionMismatch(two, one, true, 1, 1, this.describer) }, this.comparer
        .getMismatches(new Object[] { one, two, new MyObj("c") }, new Object[] { two, one, new MyObj("c") }));
  }

  public void testUsesEqualityComparator() throws Exception {
    MyObj uppercase = new MyObj("FOO");
    MyObj lowercase = new MyObj("foo");

    CASE_INSENSITIVE = false;
    checkMismatches(new CollectionMismatch[] { new UnequalObjectCollectionMismatch(uppercase, lowercase, true, 0, 0,
                                                                                   this.describer) }, this.comparer
        .getMismatches(new Object[] { uppercase }, new Object[] { lowercase }));

    CASE_INSENSITIVE = true;

    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[] { uppercase }, new Object[] { lowercase }));

    CASE_INSENSITIVE = false;
    checkMismatches(new CollectionMismatch[] { new UnequalObjectCollectionMismatch(uppercase, lowercase, true, 0, 0,
                                                                                   this.describer) }, this.comparer
        .getMismatches(new Object[] { uppercase }, new Object[] { lowercase }));
  }

  private static class Diff implements Differenceable {
    private final Object a;
    private final Object b;
    private final Object c;

    public Diff(Object a, Object b, Object c) {
      this.a = a;
      this.b = b;
      this.c = c;
    }

    public boolean equals(Object that) {
      if (!(that instanceof Diff)) return false;

      Diff diffThat = (Diff) that;

      return new EqualsBuilder().append(this.a, diffThat.a).append(this.b, diffThat.b).append(this.c, diffThat.c)
          .isEquals();
    }

    public void addDifferences(DifferenceContext context, Object that) {
      new DifferenceBuilder(context).reflectionDifference(this, that);
    }

    public String toString() {
      return "<Diff>";
    }
  }
  
  public void testDifferenceable() throws Exception {
    Diff a = new Diff("foo", "bar", "baz");
    Diff b = new Diff("foo", "bar", "quux");
    
    CollectionMismatch[] mismatches = this.comparer.getMismatches(new Object[] { a }, new Object[] { b });
    assertEquals(1, mismatches.length);
    assertTrue(mismatches[0].toString().indexOf("baz") >= 0);
    assertTrue(mismatches[0].toString().indexOf("quux") >= 0);
    assertTrue(mismatches[0].toString().indexOf("foo") < 0);
    assertTrue(mismatches[0].toString().indexOf("bar") < 0);
  }

  public void testDifferentObjectTypes() throws Exception {
    Object oneObj = new MyObj("foo");
    Object twoObj = "foo";

    checkMismatches(new CollectionMismatch[] { new UnequalObjectCollectionMismatch(oneObj, twoObj, true, 0, 0,
                                                                                   this.describer) }, this.comparer
        .getMismatches(new Object[] { oneObj }, new Object[] { twoObj }));

    checkMismatches(new CollectionMismatch[] { new UnequalObjectCollectionMismatch(twoObj, oneObj, true, 0, 0,
                                                                                   this.describer) }, this.comparer
        .getMismatches(new Object[] { twoObj }, new Object[] { oneObj }));
  }

  public void testMultipleProblems() throws Exception {
    MyObj firstZero = new MyObj("a");
    MyObj secondZero = new MyObj("x");
    MyObj bothOne = new MyObj("b");
    MyObj firstTwo = new MyObj("c");
    MyObj secondTwo = new MyObj("y");
    MyObj bothThree = new MyObj("d");
    MyObj secondFour = new MyObj("q");

    Object[] one = new Object[] { firstZero, bothOne, firstTwo, bothThree };
    Object[] two = new Object[] { secondZero, bothOne, secondTwo, bothThree, secondFour };

    CollectionMismatch[] expectedMismatches = new CollectionMismatch[] {
        new UnequalObjectCollectionMismatch(firstZero, secondZero, true, 0, 0, this.describer),
        new UnequalObjectCollectionMismatch(firstTwo, secondTwo, true, 2, 2, this.describer),
        new MissingObjectCollectionMismatch(secondFour, false, 4, this.describer) };

    checkMismatches(expectedMismatches, this.comparer.getMismatches(one, two));
  }

}