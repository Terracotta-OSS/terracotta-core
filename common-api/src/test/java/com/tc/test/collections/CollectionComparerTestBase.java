/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.collections;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.test.TCTestCase;
import com.tc.util.EqualityComparator;
import com.tc.util.Stringifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Base for all tests that test {@link CollectionComparer}s.
 */
public class CollectionComparerTestBase extends TCTestCase {

  protected static boolean CASE_INSENSITIVE = false;

  protected static class MyObj {
    public final String value;

    public MyObj(String value) {
      this.value = value;
    }

    public String toString() {
      return "X" + this.value + "Y";
    }

    public int hashCode() {
      return new HashCodeBuilder().append(this.value).toHashCode();
    }
  }

  private static class MyStringifier implements Stringifier {
    private static String PREFIX = "A";
    private static String SUFFIX = "B";

    public String toString(Object o) {
      if (o == null) return "__NULL__";
      else return PREFIX + o.toString() + SUFFIX;
    }
  }

  private static class MyComparator implements EqualityComparator {
    public boolean isEquals(Object one, Object two) {
      if ((one == null) != (two == null)) return false;
      if (one == null) return true;

      if (!(one instanceof MyObj)) return false;
      if (!(two instanceof MyObj)) return false;

      MyObj myOne = (MyObj) one;
      MyObj myTwo = (MyObj) two;

      if (CASE_INSENSITIVE) return myOne.value.equalsIgnoreCase(myTwo.value);
      else return myOne.value.equals(myTwo.value);
    }
  }

  protected static final CollectionMismatch[] NO_MISMATCHES = new CollectionMismatch[0];

  protected Stringifier                       describer;
  protected EqualityComparator                equalityComparator;
  protected CollectionComparer                comparer;

  public void setUp() throws Exception {
    this.describer = new MyStringifier();
    this.equalityComparator = new MyComparator();
  }

  public void testEmpty() throws Exception {
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[0], new Object[0]));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[0], iterator(new Object[0])));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[0], list(new Object[0])));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[0], set(new Object[0])));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(iterator(new Object[0]), new Object[0]));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(list(new Object[0]), new Object[0]));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(set(new Object[0]), new Object[0]));
  }

  public void testSingleEquals() throws Exception {
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[] { new MyObj("foo") },
                                                               new Object[] { new MyObj("foo") }));
  }

  public void testDifferentCollectionTypes() throws Exception {
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[] { new MyObj("foo") },
                                                               new Object[] { new MyObj("foo") }));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[] { new MyObj("foo") },
                                                               iterator(new Object[] { new MyObj("foo") })));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[] { new MyObj("foo") },
                                                               list(new Object[] { new MyObj("foo") })));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(new Object[] { new MyObj("foo") },
                                                               set(new Object[] { new MyObj("foo") })));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(iterator(new Object[] { new MyObj("foo") }),
                                                               new Object[] { new MyObj("foo") }));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(list(new Object[] { new MyObj("foo") }),
                                                               new Object[] { new MyObj("foo") }));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(set(new Object[] { new MyObj("foo") }),
                                                               new Object[] { new MyObj("foo") }));
  }

  public void testSingleDoesNotEqualNothing() throws Exception {
    MyObj missingObj = new MyObj("foo");

    checkMismatches(
                    new CollectionMismatch[] { new MissingObjectCollectionMismatch(missingObj, true, 0, this.describer) },
                    this.comparer.getMismatches(new Object[] { missingObj }, new Object[0]));
  }

  public void testNothingDoesNotEqualSingle() throws Exception {
    MyObj missingObj = new MyObj("foo");

    checkMismatches(
                    new CollectionMismatch[] { new MissingObjectCollectionMismatch(missingObj, false, 0, this.describer) },
                    this.comparer.getMismatches(new Object[0], new Object[] { missingObj }));
  }

  public void testSameCollections() throws Exception {
    Object[] collectionOne = new Object[] { new MyObj("a"), new MyObj("q"), new MyObj("c"), new MyObj("BBBBBBB") };
    Object[] collectionTwo = new Object[] { new MyObj("a"), new MyObj("q"), new MyObj("c"), new MyObj("BBBBBBB") };

    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(collectionOne, collectionTwo));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(collectionTwo, collectionOne));

    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(iterator(collectionOne), collectionTwo));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(iterator(collectionTwo), collectionOne));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(collectionOne, iterator(collectionTwo)));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(collectionTwo, iterator(collectionOne)));

    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(list(collectionOne), collectionTwo));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(list(collectionTwo), collectionOne));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(collectionOne, list(collectionTwo)));
    checkMismatches(NO_MISMATCHES, this.comparer.getMismatches(collectionTwo, list(collectionOne)));
  }

  public void testUsesStringifier() throws Exception {
    String oldPrefix = MyStringifier.PREFIX;
    String oldSuffix = MyStringifier.SUFFIX;

    try {
      MyStringifier.PREFIX = "FOO";
      MyStringifier.SUFFIX = "BAR";

      CollectionMismatch[] actual = this.comparer.getMismatches(new Object[] { new MyObj("P") },
                                                                new Object[] { new MyObj("Q") });

      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < actual.length; ++i) {
        buf.append(actual[i].toString());
      }

      String theString = buf.toString();

      assertTrue(theString.indexOf("FOOXPYBAR") >= 0);
      assertTrue(theString.indexOf("FOOXQYBAR") >= 0);
    } finally {
      MyStringifier.PREFIX = oldPrefix;
      MyStringifier.SUFFIX = oldSuffix;
    }
  }

  public void testExtraElements() throws Exception {
    MyObj extra = new MyObj("qz");

    checkMismatches(new CollectionMismatch[] { new MissingObjectCollectionMismatch(extra, true, 1, this.describer) },
                    this.comparer
                        .getMismatches(new Object[] { new MyObj("a"), extra }, new Object[] { new MyObj("a") }));

    checkMismatches(new CollectionMismatch[] { new MissingObjectCollectionMismatch(extra, false, 1, this.describer) },
                    this.comparer
                        .getMismatches(new Object[] { new MyObj("a") }, new Object[] { new MyObj("a"), extra }));
  }

  public void testChecksArguments() throws Exception {
    try {
      this.comparer.getMismatches(new Object[0], null);
      fail("Didn't get NPE on no second collection");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      this.comparer.getMismatches(null, new Object[0]);
      fail("Didn't get NPE on no first collection");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      this.comparer.getMismatches("foo", new Object[0]);
      fail("Didn't get IAE on bogus first argument");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      this.comparer.getMismatches(new Object[0], "foo");
      fail("Didn't get IAE on bogus second argument");
    } catch (IllegalArgumentException iae) {
      // ok
    }
  }

  private Iterator iterator(Object[] data) {
    return Arrays.asList(data).iterator();
  }

  private List list(Object[] data) {
    ArrayList out = new ArrayList();
    out.addAll(Arrays.asList(data));
    return out;
  }

  private Set set(Object[] data) {
    HashSet out = new HashSet();
    out.addAll(Arrays.asList(data));
    return out;
  }

  // NOTE 2004-12-27 andrew -- This compares in an ordered fashion. This could cause problems if the implementation is
  // changed to return CollectionMismatches in a different order; if so, we'll need to enhance this method to not care
  // about order. However, we should NOT use a CollectionComparer to do that, for the obvious reasons -- you don't want
  // to use the code you're testing as part of the test's infrastructure itself.
  protected final void checkMismatches(CollectionMismatch[] expected, CollectionMismatch[] actual) {
    StringBuffer out = new StringBuffer();

    for (int i = 0; i < expected.length; ++i) {
      if (i >= actual.length) {
        out.append("Missing an expected mismatch at index " + i + ": " + expected[i] + "\n");
      } else if (!expected[i].equals(actual[i])) {
        out.append("Wrong mismatch at index " + i + ": expected " + expected[i] + ", but got " + actual[i] + "\n");
      }
    }

    if (actual.length > expected.length) {
      for (int i = expected.length; i < actual.length; ++i) {
        out.append("Got an unexpected mismatch at index " + i + ": " + actual[i]);
      }
    }

    if (out.toString().length() > 0) {
      fail(out.toString());
    }
  }

}