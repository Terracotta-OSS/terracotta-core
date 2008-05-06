/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.collections;

import com.tc.util.EqualityComparator;
import com.tc.util.EqualsEqualityComparator;
import com.tc.util.StandardStringifier;
import com.tc.util.Stringifier;

import junit.framework.Assert;

/**
 * A set of primitives for comparing collections in tests.
 */
public class CollectionAssert {

  private static final int                MAX_LENGTH_TO_SHOW_COLLECTIONS   = 500;
  private static final int                MAX_NEWLINES_TO_SHOW_COLLECTIONS = 15;

  private static final EqualityComparator STANDARD_COMPARATOR              = EqualsEqualityComparator.INSTANCE;
  private static final Stringifier        STANDARD_STRINGIFIER             = StandardStringifier.INSTANCE;

  public static void assertEqualUnorderedUncounted(Object one, Object two) {
    assertEqualUnorderedUncounted(one, two, STANDARD_STRINGIFIER);
  }

  public static void assertEqualUnorderedUncounted(Object one, Object two, Stringifier stringifier) {
    assertEqualUnorderedUncounted(one, two, STANDARD_COMPARATOR, stringifier);
  }

  public static void assertEqualUnorderedUncounted(Object one, Object two, EqualityComparator comparator) {
    assertEqualUnorderedUncounted(one, two, comparator, STANDARD_STRINGIFIER);
  }

  public static void assertEqualUnorderedUncounted(Object one, Object two, EqualityComparator comparator,
                                                   Stringifier stringifier) {
    assertEqual(one, two, new UnorderedUncountedCollectionComparer(comparator, stringifier), stringifier);
  }

  public static void assertEqualUnordered(Object one, Object two) {
    assertEqualUnordered(one, two, STANDARD_STRINGIFIER);
  }

  public static void assertEqualUnordered(Object one, Object two, Stringifier stringifier) {
    assertEqualUnordered(one, two, STANDARD_COMPARATOR, stringifier);
  }

  public static void assertEqualUnordered(Object one, Object two, EqualityComparator comparator) {
    assertEqualUnordered(one, two, comparator, STANDARD_STRINGIFIER);
  }

  public static void assertEqualUnordered(Object one, Object two, EqualityComparator comparator, Stringifier stringifier) {
    assertEqual(one, two, new UnorderedCollectionComparer(comparator, stringifier), stringifier);
  }

  public static void assertEqualOrdered(Object one, Object two) {
    assertEqualOrdered(one, two, STANDARD_STRINGIFIER);
  }

  public static void assertEqualOrdered(Object one, Object two, Stringifier stringifier) {
    assertEqualOrdered(one, two, STANDARD_COMPARATOR, stringifier);
  }

  public static void assertEqualOrdered(Object one, Object two, EqualityComparator comparator) {
    assertEqualOrdered(one, two, comparator, STANDARD_STRINGIFIER);
  }

  public static void assertEqualOrdered(Object one, Object two, EqualityComparator comparator, Stringifier stringifier) {
    assertEqual(one, two, new OrderedCollectionComparer(comparator, stringifier), stringifier);
  }

  private static void assertEqual(Object one, Object two, CollectionComparer comparer, Stringifier describer) {
    com.tc.util.Assert.assertNotNull(one);
    com.tc.util.Assert.assertNotNull(two);

    String oneString = describer.toString(one);
    String twoString = describer.toString(two);

    int totalLength = oneString.length() + twoString.length();
    int totalNewlines = countNewlines(oneString) + countNewlines(twoString);

    boolean showCollections = (totalLength <= MAX_LENGTH_TO_SHOW_COLLECTIONS)
                              && (totalNewlines <= MAX_NEWLINES_TO_SHOW_COLLECTIONS);

    assertEqual(one, two, comparer, describer, showCollections);
  }

  private static int countNewlines(String s) {
    int out = 0;
    int length = s.length();

    boolean justWasCR = false;

    for (int i = 0; i < length; ++i) {
      char ch = s.charAt(i);
      if (ch == '\r' || (ch == '\n' && (!justWasCR))) {
        ++out;
      }

      justWasCR = (ch == '\r');
    }

    return out;
  }

  private static void assertEqual(Object one, Object two, CollectionComparer comparer, Stringifier describer,
                                  boolean showCollections) {
    com.tc.util.Assert.assertNotNull(one);
    com.tc.util.Assert.assertNotNull(two);

    CollectionMismatch[] mismatches = comparer.getMismatches(one, two);
    if (mismatches.length > 0) {
      StringBuffer descrip = new StringBuffer();

      if (showCollections) {
        descrip
            .append("Collections " + describer.toString(one) + " and " + describer.toString(two) + " aren't equal: ");
      } else {
        descrip.append("Collections aren't equal:");
      }

      for (int i = 0; i < mismatches.length; ++i) {
        descrip.append(mismatches[i].toString());
        descrip.append("\n");
      }

      Assert.fail(descrip.toString());
    }
  }

}