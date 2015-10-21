/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

package com.tc.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Stores an ordered list, 0-indexed by int, with fixed indices within a sparse space.
 * That is, there can be gaps in the array and attempts to insert new elements will not change the relative order of other elements (instead, replacing any already present at that index).
 * The iterator for the list will walk the list, in order, skipping any holes. 
 */
public class SparseList<T> implements Iterable<T> {
  private final Map<Integer, T> map = new HashMap<>();
  private int lastIndex = -1;
  
  /**
   * Inserts object at index, returning the object formerly at that index (returns null if the index was empty).
   */
  public T insert(int index, T object) {
    if (index > this.lastIndex) {
      this.lastIndex = index;
    }
    return this.map.put(index, object);
  }

  @Override
  public Iterator<T> iterator() {
    return new SparseListIterator<>(this.map, this.lastIndex);
  }


  /**
   * The iterator for walking a sparse list.
   */
  private static class SparseListIterator<T> implements Iterator<T> {
    private final Map<Integer, T> map;
    private final int lastIndex;
    private int nextIndex;

    public SparseListIterator(Map<Integer, T> map, int lastIndex) {
      this.map = map;
      this.lastIndex = lastIndex;
      this.nextIndex = 0;
    }
    @Override
    public boolean hasNext() {
      return (this.nextIndex <= this.lastIndex);
    }
    @Override
    public T next() {
      T next = null;
      while (null == next) {
        // This can only happen if next() was called when hasNext() is false and it would result in looping until overflow so assert, instead.
        Assert.assertTrue(this.nextIndex <= this.lastIndex);
        next = this.map.get(this.nextIndex);
        this.nextIndex += 1;
      }
      return next;
    }
  }
}
