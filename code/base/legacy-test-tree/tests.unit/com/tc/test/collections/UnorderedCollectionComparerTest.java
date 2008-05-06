/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.collections;

/**
 * Unit test for {@link UnorderedCollectionComparer}.
 */
public class UnorderedCollectionComparerTest extends CollectionComparerTestBase {

  public void setUp() throws Exception {
    super.setUp();
    this.comparer = new UnorderedCollectionComparer(this.equalityComparator, this.describer);
  }

  public void testDoesNotCheckOrder() throws Exception {
    MyObj one = new MyObj("a");
    MyObj two = new MyObj("b");

    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[] { one, two, new MyObj("c") }, new Object[] {
        two, one, new MyObj("c") }));
  }

  public void testChecksCounts() throws Exception {
    MyObj one = new MyObj("a");
    MyObj two = new MyObj("b");

    checkMismatches(new CollectionMismatch[] { new UnequalObjectCountCollectionMismatch(one, 0, 2, 1, this.describer),
        new UnequalObjectCountCollectionMismatch(two, 2, 1, 2, this.describer) }, this.comparer
        .getMismatches(new Object[] { one, one, two, new MyObj("c") }, new Object[] { two, two, one, new MyObj("c") }));
  }

  public void testUsesEqualityComparator() throws Exception {
    MyObj uppercase = new MyObj("FOO");
    MyObj lowercase = new MyObj("foo");

    CASE_INSENSITIVE = false;
    checkMismatches(new CollectionMismatch[] { new MissingObjectCollectionMismatch(uppercase, true, 0, this.describer),
        new MissingObjectCollectionMismatch(lowercase, false, 0, this.describer) }, this.comparer
        .getMismatches(new Object[] { uppercase }, new Object[] { lowercase }));

    CASE_INSENSITIVE = true;

    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[] { uppercase }, new Object[] { lowercase }));

    CASE_INSENSITIVE = false;
    checkMismatches(new CollectionMismatch[] { new MissingObjectCollectionMismatch(uppercase, true, 0, this.describer),
        new MissingObjectCollectionMismatch(lowercase, false, 0, this.describer) }, this.comparer
        .getMismatches(new Object[] { uppercase }, new Object[] { lowercase }));
  }

  public void testDifferentObjectTypes() throws Exception {
    Object oneObj = new MyObj("foo");
    Object twoObj = "foo";

    checkMismatches(new CollectionMismatch[] { new MissingObjectCollectionMismatch(oneObj, true, 0, this.describer),
        new MissingObjectCollectionMismatch(twoObj, false, 0, this.describer) }, this.comparer
        .getMismatches(new Object[] { oneObj }, new Object[] { twoObj }));

    checkMismatches(new CollectionMismatch[] { new MissingObjectCollectionMismatch(twoObj, true, 0, this.describer),
        new MissingObjectCollectionMismatch(oneObj, false, 0, this.describer) }, this.comparer
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
        new MissingObjectCollectionMismatch(firstZero, true, 0, this.describer),
        new MissingObjectCollectionMismatch(firstTwo, true, 2, this.describer),
        new MissingObjectCollectionMismatch(secondZero, false, 0, this.describer),
        new MissingObjectCollectionMismatch(secondTwo, false, 2, this.describer),
        new MissingObjectCollectionMismatch(secondFour, false, 4, this.describer) };

    checkMismatches(expectedMismatches, this.comparer.getMismatches(one, two));
  }

}