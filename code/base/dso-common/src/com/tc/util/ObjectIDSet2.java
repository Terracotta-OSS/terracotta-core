/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class is a an attempt to meet the shortcomings of ObjectIDSet. Mainly the performance of adds and removes when
 * the ObjectIDs are non-contiguous. It uses a balanced tree internally to store ranges instead of an arraylist
 */
public class ObjectIDSet2 extends AbstractSet {

  private final AATree ranges;
  private int          size = 0;

  public ObjectIDSet2() {
    ranges = new AATree();
  }

  public ObjectIDSet2(Collection c) {
    this();
    if (c instanceof ObjectIDSet2) {
      ObjectIDSet2 other = (ObjectIDSet2) c;
      // fast way to clone
      this.size = other.size();
      for (Iterator i = other.ranges.iterator(); i.hasNext();) {
        this.ranges.insert((Range) ((Range) i.next()).clone());
      }
      return;
    } else {
      addAll(c);
    }
  }

  public Iterator iterator() {
    return new ObjectIDSetIterator();
  }

  public int size() {
    return size;
  }

  public boolean contains(ObjectID id) {
    long lid = id.toLong();
    return (ranges.find(new MyLong(lid)) != null);
  }

  public boolean remove(ObjectID id) {
    long lid = id.toLong();

    Range current = (Range) ranges.find(new MyLong(lid));
    if (current == null) {
      // Not found
      return false;
    }
    Range newRange = current.remove(lid);
    if (newRange != null) {
      ranges.insert(newRange);
    } else if (current.isNull()) {
      ranges.remove(current);
    }
    size--;
    return true;
  }

  public boolean add(ObjectID id) {
    long lid = id.toLong();

    // Step 1 : Check if the previous number is present, if so add to the same Range.
    Range prev = (Range) ranges.find(new MyLong(lid - 1));
    if (prev != null) {
      boolean isAdded = prev.add(lid);
      if (isAdded) {
        Range next = (Range) ranges.remove((new MyLong(lid + 1)));
        if (next != null) prev.merge(next);
        size++;
      }
      return isAdded;
    }

    // Step 2 : Check if the next number is present, if so add to the same Range.
    Range next = (Range) ranges.find((new MyLong(lid + 1)));
    if (next != null) {
      boolean isAdded = next.add(lid);
      if (isAdded) size++;
      return isAdded;
    }

    // Step 3: Add a new range for just this number.
    boolean isAdded = ranges.insert(new Range(lid, lid));
    if (isAdded) size++;
    return isAdded;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("ObjectIDSet2 [");
    for (Iterator i = ranges.iterator(); i.hasNext();) {
      sb.append(' ').append(i.next());
    }
    return sb.append(']').toString();
  }

  /**
   * Ranges store the elements stored in the tree. The range is inclusive.
   */
  private static class Range implements Cloneable, Comparable {
    public long start;
    public long end;

    public String toString() {
      return "Range(" + start + "," + end + ")";
    }

    public boolean isNull() {
      return start > end;
    }

    public Range remove(long lid) {
      if (lid < start || lid > end) { throw new AssertionError("Ranges : Illegal value passed to remove : " + this
                                                               + " remove called for : " + lid); }
      if (start == lid) {
        start++;
        return null;
      } else if (end == lid) {
        end--;
        return null;
      } else {
        Range newRange = new Range(lid + 1, end);
        end = lid - 1;
        return newRange;
      }
    }

    public void merge(Range other) {
      if (start == other.end + 1) {
        start = other.start;
      } else if (end == other.start - 1) {
        end = other.end;
      } else {
        throw new AssertionError("Ranges : Merge is called on non contigues value : " + this + " and other Range is "
                                 + other);
      }
    }

    public boolean add(long lid) {
      if (lid == start - 1) {
        start--;
        return true;
      } else if (lid == end + 1) {
        end++;
        return true;
      } else if (lid >= start && lid <= end) {
        return false;
      } else {
        throw new AssertionError("Ranges : Add is called on non contigues value : " + this + " but trying to add "
                                 + lid);
      }
    }

    public Range(long start, long end) {
      this.start = start;
      this.end = end;
    }

    public Object clone() {
      return new Range(start, end);
    }

    public int compareTo(Object o) {
      if (o instanceof Range) {
        Range other = (Range) o;
        if (start < other.start) return -1;
        else if (start == other.start) return 0;
        else return 1;
      } else {
        long n = ((MyLong) o).longValue();
        if (end < n) return -1;
        else if (n < start) return 1;
        else return 0;
      }
    }
  }

  // This class is used as a key for lookup.
  private static final class MyLong implements Comparable {
    final long number;

    public MyLong(long number) {
      this.number = number;
    }

    public long longValue() {
      return number;
    }

    public int compareTo(Object o) {
      if (o instanceof Range) {
        Range r = (Range) o;
        if (number < r.start) return -1;
        else if (number > r.end) return 1;
        else return 0;
      } else {
        long other = ((MyLong) o).longValue();
        if (number < other) return -1;
        else if (number > other) return 1;
        else return 0;
      }
    }
    
    public String toString() {
      return "MyLong@" + System.identityHashCode(this) + "(" + number +")" ;
    }
  }

  private class ObjectIDSetIterator implements Iterator {

    Iterator nodes;
    Range    current;
    int      idx = 0;

    public ObjectIDSetIterator() {
      nodes = ranges.iterator();
      if (nodes.hasNext()) current = (Range) nodes.next();
    }

    public boolean hasNext() {
      return nodes.hasNext() || (current != null && (current.start + idx) <= current.end);
    }

    public Object next() {
      if (current == null) throw new NoSuchElementException();
      ObjectID oid = new ObjectID(current.start + idx);
      if (current.start + idx == current.end) {
        idx = 0;
        if (nodes.hasNext()) {
          current = (Range) nodes.next();
        } else {
          current = null;
        }
      } else {
        idx++;
      }
      return oid;
    }

    // This is a little tricky as the tree might balance itself.
    public void remove() {
      throw new ImplementMe();
    }
  }

  /*
   * Because of the short comings of the iterator (it cant perform remove), this method is overridden
   */
  public boolean removeAll(Collection c) {
    boolean modified = false;
    if (size() > c.size()) {
      for (Iterator i = c.iterator(); i.hasNext();)
        modified |= remove(i.next());
    } else {
      // XXX :; yuck !!
      ArrayList toRemove = new ArrayList();
      for (Iterator i = iterator(); i.hasNext();) {
        Object o = i.next();
        if (c.contains(o)) {
          toRemove.add(o);
          modified = true;
        }
      }
      for (Iterator i = toRemove.iterator(); i.hasNext();) {
        remove(i.next());
      }
    }
    return modified;
  }

  /*
   * Because of the short comings of the iterator (it cant perform remove), this method is overridden
   */
  public boolean retainAll(Collection c) {
    boolean modified = false;
      // XXX :; yuck !!
    ArrayList toRemove = new ArrayList(c.size());
    Iterator e = iterator();
    while (e.hasNext()) {
      Object o = e.next();
      if (!c.contains(o)) {
        toRemove.add(o);
        modified = true;
      }
    }
    for (Iterator i = toRemove.iterator(); i.hasNext();) {
      remove(i.next());
    }
    return modified;
  }

  public boolean contains(Object o) {
    if (o instanceof ObjectID) {
      return contains((ObjectID) o);
    } else {
      return false;
    }
  }

  public boolean add(Object arg0) {
    return add((ObjectID) arg0);
  }

  public boolean remove(Object o) {
    if (o instanceof ObjectID) {
      return remove((ObjectID) o);
    } else {
      return false;
    }
  }

  public void clear() {
    this.size = 0;
    ranges.clear();
  }

}
