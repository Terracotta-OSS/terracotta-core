/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * TODO May 31, 2006: 1) Make this set special case things like addAll() removeAll() etc if the passed in collection is
 * also an ObjectIDSet 2) Make this set optimized for worst case scenario too. (like for storing ObjectIDs 1, 3, 5, 7, 9
 * etc.
 */
public class ObjectIDSet extends AbstractSet implements Cloneable {
  private final List ranges;
  private int        size = 0;

  public ObjectIDSet() {
    ranges = new ArrayList();
//    ranges = new LinkedList();
  }

  public ObjectIDSet(Collection c) {
    if (c instanceof ObjectIDSet) {
      ObjectIDSet oidSet = (ObjectIDSet) c;
      // fast way to clone
      size = oidSet.size();
      ranges = new ArrayList(oidSet.ranges.size());
      for (int i = 0; i < oidSet.ranges.size(); i++) {
        ranges.add(((Range) oidSet.ranges.get(i)).clone());
      }
      return;
    } else {
      ranges = new ArrayList();
      addAll(c);
    }
  }

  public Object clone() {
    return new ObjectIDSet(this);
  }

  public Iterator iterator() {
    return new ObjectIDSetIterator();
  }

  public int size() {
    return size;
  }

  public boolean remove(ObjectID id) {
    long lid = id.toLong();
    int index = findIndex(lid);
    if (index < 0) return false;
    return removeObjectIDAt(lid, index);
  }

  // THis is a private method and no checks are done
  private boolean removeObjectIDAt(long lid, int index) {
    size--;
    Range r = (Range) ranges.get(index);
    if (r.start == lid && r.end == lid + 1) {
      ranges.remove(index);
      return true;
    }

    if (r.start == lid) {
      r.start = r.start + 1;
    } else if (r.end == lid + 1) {
      r.end = r.end - 1;
    } else if(r.start < lid && lid < r.end ) {
      Range newRange = new Range(lid + 1, r.end);
      r.end = lid;

      Assert.eval(newRange.start != newRange.end);
      ranges.add(index + 1, newRange);
    } else {
      Assert.failure("Called with the wrong the index : " + index + " for lid : " + lid);
    }

    return true;
  }

  public boolean add(ObjectID id) {
    long lid = id.toLong();

    int index = 0;
    int low = 0;
    int high = ranges.size() - 1;

    // if it's empty add and retrun;
    if (ranges.size() == 0) {
      ranges.add(index, new Range(lid, lid + 1));
      size++;
      return true;
    }

    // If it's an add at the end;
    Range lr = (Range) ranges.get(high);
    if (lid == lr.end) {
      lr.end++;
      size++;
      return true;
    }

    // if this thing goes on the end just add it
    if (lr.end + 1 < lid) {
      ranges.add(new Range(lid, lid + 1));
      size++;
      return true;
    }

    Range fr = (Range) ranges.get(0);
    if (lid < fr.start - 1) {
      ranges.add(index, new Range(lid, lid + 1));
      size++;
      return true;
    }

    while (low <= high) {
      index = low + high >> 1;
      Range r = (Range) ranges.get(index);

      if (r.end < lid) {
        low = index + 1;
      } else {
        high = index - 1;
      }

      if (r.contains(lid)) return false;// exists
      if (r.end == lid) {
        r.addToEnd();
        while (++index < ranges.size()) {
          Range n = (Range) ranges.get(index);
          if (n.start == r.end) {
            r.end = n.end;
            ranges.remove(index);
          } else {
            break;
          }
        }
        size++;
        return true;
      }
      if (r.start - 1 == lid) {
        r.addToStart();
        size++;
        while (--index >= 0) {
          Range pr = (Range) ranges.get(index);
          if (pr.end == r.start) {
            r.start = pr.start;
            ranges.remove(index);
          } else {
            break;
          }
        }
        return true;
      }
    }
    size++;
    Range ir = (Range) ranges.get(index);
    Range newRange = new Range(lid, lid + 1);
    if (ir.end < lid) {
      ranges.add(index + 1, newRange);
    } else {
      ranges.add(index, newRange);
    }
    return true;
  }

  public static class Range implements Cloneable {
    public long start;
    public long end;

    public String toString() {
      return "Range(" + start + "," + end + ")";
    }

    public Range(long start, long end) {
      this.start = start;
      this.end = end;
    }

    public void addToEnd() {
      end++;
    }

    public void addToStart() {
      start--;
    }

    public boolean contains(long id) {
      return id >= start && id < end;
    }

    public Object clone() {
      return new Range(start, end);
    }
  }

  private int findIndex(long lid) {

    int index = 0;
    int low = 0;
    int high = ranges.size() - 1;

    // if it's empty add and retrun;
    if (ranges.size() == 0) { return -1; }

    // if this thing goes on the end
    Range lr = (Range) ranges.get(ranges.size() - 1);
    if (lr.end <= lid) { return -1; }
    if (lr.contains(lid)) { return high; }

    Range fr = (Range) ranges.get(0);
    if (lid < fr.start) { return -1; }
    if (fr.contains(lid)) return 0;

    while (low <= high) {
      index = low + high >> 1;
      Range r = (Range) ranges.get(index);

      if (r.end < lid) {
        low = index + 1;
      } else {
        high = index - 1;
      }

      if (r.contains(lid)) return index;
    }
    return -1;
  }

  public class ObjectIDSetIterator implements Iterator {
    private int      range;
    private int      offset;
    private ObjectID current;
    public int       visited = 0;

    public boolean hasNext() {
      return !(range >= ranges.size());
    }

    public Object next() {
      if (range >= ranges.size()) throw new NoSuchElementException();
      visited++;
      Range r = (Range) ranges.get(range);
      current = new ObjectID(r.start + offset);
      if (r.start + offset + 1 < r.end) {
        offset++;
      } else {
        offset = 0;
        range++;
      }

      return current;
    }

    public void remove() {
      if (current == null) throw new IllegalStateException();
      int b4size = ranges.size();
      // ObjectIDSet.this.remove(current);
      ObjectIDSet.this.removeObjectIDAt(current.toLong(), (offset == 0 ? range -1 : range));
      if (b4size > ranges.size()) {
        // Case 1: Last returned ObjectID was the only one contained in that Range.
        Assert.assertEquals(0, offset);
        range--;
      } else if (b4size == ranges.size()) {
        // Case 2: Last returned ObjectID was either the first or the last of the range.
        if (offset == 1) {
          // It was the first
          offset = 0;
        } else {
          // It was the last. So no change
          Assert.assertEquals(0, offset);
        }
      } else {
        // Case 3 : b4size < ranges.size(); Last returned ObjectIDSet was some where in the middle.
        range++;
        Assert.assertTrue(offset != 0);
        offset = 0;
      }
    }
  }

  public boolean contains(Object o) {
    return findIndex(((ObjectID) o).toLong()) >= 0;
  }

  public boolean add(Object arg0) {
    return add((ObjectID) arg0);
  }

  public boolean remove(Object o) {
    return remove((ObjectID) o);
  }

  public void clear() {
    this.size = 0;
    ranges.clear();
  }
}
