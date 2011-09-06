/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.collections;

/**
 * Unit test for {@link UnorderedUncountedCollectionComparer}.
 */
public class UnorderedUncountedCollectionComparerTest extends UnorderedCollectionComparerTest {

  public void setUp() throws Exception {
    super.setUp();
    this.comparer = new UnorderedUncountedCollectionComparer(this.equalityComparator, this.describer);
  }

  public void testChecksCounts() throws Exception {
    MyObj one = new MyObj("a");
    MyObj two = new MyObj("b");

    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[] { one, one, two, new MyObj("c") },
                                                               new Object[] { two, two, one, new MyObj("c") }));
  }

}