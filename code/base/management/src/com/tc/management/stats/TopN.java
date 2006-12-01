/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.stats;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Keeps track of the top <strong>N</strong> entries of all those sumbitted according to their natural order, or as
 * defined by the passed in {@link Comparator} if the second constructor is used. <strong>The comparator should order
 * the elements from lowest to highest, otherwise you will wind up </strong>
 */
public final class TopN {

  private final SortedSet data;
  private final int       N;

  /**
   * Creates a top {@link #N} list according to a natural ordering, if you have a custom object you will be adding use
   * {@link #TopN(Comparator, int)} instead.
   */
  public TopN(final int N) {
    this(null, N);
  }

  /**
   * Creates a top {@link N} list according to {@link comparator}.
   */
  public TopN(final Comparator comparator, final int N) {
    data = new TreeSet(comparator != null ? new Comparator() {
      public int compare(Object obj, Object obj1) {
        return -comparator.compare(obj, obj1);
      }
    } : Collections.reverseOrder());
    this.N = N;
  }

  /**
   * @param object the object added to the top N list in order, if it qualifies according to the comparator
   */
  public void evaluate(final Object object) {
    synchronized (data) {
      data.add(object);
      while (data.size() > N) {
        data.remove(data.last());
      }
    }
  }

  /**
   * @return a read-only {@link Iterator} of the "top" {@link #N} elements, in descending order -- that is, the
   *         "biggest" one is first.
   */
  public Iterator iterator() {
    synchronized (data) {
      return Collections.unmodifiableSortedSet(data).iterator();
    }
  }

}
