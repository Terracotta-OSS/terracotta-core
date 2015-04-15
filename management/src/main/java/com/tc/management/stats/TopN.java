/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.management.stats;

import java.util.ArrayList;
import java.util.Collection;
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
  public boolean evaluate(final Object object) {
    synchronized (data) {
      data.add(object);
      while (data.size() > N) {
        data.remove(data.last());
      }
      return data.contains(object);
    }
  }
  
  public void evaluate(final Collection objects) {
    for (Iterator i=objects.iterator(); i.hasNext(); ) {
      evaluate(i.next());
    }
  }
  
  public boolean remove(final Object object) {
    synchronized(data) {
      return data.remove(object);
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
  
  public Collection getDataSnapshot() {
    synchronized(data) {
      return new ArrayList(data);
    }
  }

}
