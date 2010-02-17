/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.util.AATreeSet.AANode;

import java.io.IOException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class RangeObjectIDSet extends ObjectIDSetBase {

  public RangeObjectIDSet(Collection c) {
    super();
    if (c instanceof RangeObjectIDSet) {
      RangeObjectIDSet other = (RangeObjectIDSet) c;
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

  public RangeObjectIDSet() {
    super();
  }

  @Override
  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    if (this.size != 0) { throw new RuntimeException("deserialize dirty ObjectIDSet"); }
    int _size = in.readInt();
    this.size = _size;
    while (_size > 0) {
      long start = in.readLong();
      long end = in.readLong();
      Range r = new Range(start, end);
      this.ranges.insert(r);
      _size -= r.size();
    }
    return this;
  }

  @Override
  public void serializeTo(TCByteBufferOutput out) {
    out.writeInt(this.size);
    for (Iterator i = this.ranges.iterator(); i.hasNext();) {
      Range r = (Range) i.next();
      out.writeLong(r.start);
      out.writeLong(r.end);
    }
  }

  @Override
  public boolean contains(ObjectID id) {
    long lid = id.toLong();
    return (this.ranges.find(new MyLong(lid)) != null);
  }

  @Override
  public boolean remove(ObjectID id) {
    long lid = id.toLong();

    Range current = (Range) this.ranges.find(new MyLong(lid));
    if (current == null) {
      // Not found
      return false;
    }
    Range newRange = current.remove(lid);
    if (newRange != null) {
      this.ranges.insert(newRange);
    } else if (current.isNull()) {
      this.ranges.remove(current);
    }
    this.size--;
    this.modCount++;
    return true;
  }

  @Override
  public boolean add(ObjectID id) {
    long lid = id.toLong();

    // Step 1 : Check if the previous number is present, if so add to the same Range.
    Range prev = (Range) this.ranges.find(new MyLong(lid - 1));
    if (prev != null) {
      boolean isAdded = prev.add(lid);
      if (isAdded) {
        Range next = (Range) this.ranges.remove((new MyLong(lid + 1)));
        if (next != null) {
          prev.merge(next);
        }
        this.size++;
        this.modCount++;
      }
      return isAdded;
    }

    // Step 2 : Check if the next number is present, if so add to the same Range.
    Range next = (Range) this.ranges.find((new MyLong(lid + 1)));
    if (next != null) {
      boolean isAdded = next.add(lid);
      if (isAdded) {
        this.size++;
        this.modCount++;
      }
      return isAdded;
    }

    // Step 3: Add a new range for just this number.
    boolean isAdded = this.ranges.insert(new Range(lid, lid));
    if (isAdded) {
      this.size++;
      this.modCount++;
    }
    return isAdded;
  }

  @Override
  public Iterator iterator() {
    return new RangeObjectIDSetIterator();
  }

  @Override
  public ObjectID first() {
    if (this.size == 0) { throw new NoSuchElementException(); }
    Range min = (Range) this.ranges.findMin();
    return new ObjectID(min.start);
  }

  @Override
  public ObjectID last() {
    if (this.size == 0) { throw new NoSuchElementException(); }
    Range max = (Range) this.ranges.findMax();
    return new ObjectID(max.end);
  }

  private class RangeObjectIDSetIterator implements Iterator {

    Iterator                nodes;
    private Range current;
    ObjectID                lastReturned;
    int                     idx;
    int                     expectedModCount;

    public RangeObjectIDSetIterator() {
      this.nodes = RangeObjectIDSet.this.ranges.iterator();
      this.expectedModCount = RangeObjectIDSet.this.modCount;
      this.idx = 0;
      if (this.nodes.hasNext()) {
        this.current = (Range) this.nodes.next();
      }
    }

    public boolean hasNext() {
      return this.nodes.hasNext() || (this.current != null && (this.current.start + this.idx) <= this.current.end);
    }

    public Object next() {
      if (this.current == null) { throw new NoSuchElementException(); }
      if (this.expectedModCount != RangeObjectIDSet.this.modCount) { throw new ConcurrentModificationException(); }
      ObjectID oid = new ObjectID(this.current.start + this.idx);
      if (this.current.start + this.idx == this.current.end) {
        this.idx = 0;
        if (this.nodes.hasNext()) {
          this.current = (Range) this.nodes.next();
        } else {
          this.current = null;
        }
      } else {
        this.idx++;
      }
      return (this.lastReturned = oid);
    }

    public void remove() {
      if (this.lastReturned == null) { throw new IllegalStateException(); }
      if (this.expectedModCount != RangeObjectIDSet.this.modCount) { throw new ConcurrentModificationException(); }
      RangeObjectIDSet.this.remove(this.lastReturned);
      this.expectedModCount = RangeObjectIDSet.this.modCount;
      this.nodes = RangeObjectIDSet.this.ranges.tailSetIterator(new MyLong(this.lastReturned.toLong()));
      if (this.nodes.hasNext()) {
        this.current = (Range) this.nodes.next();
        this.idx = 0; // TODO:: verify ;; has to be
      } else {
        this.current = null;
      }
      this.lastReturned = null;
    }
  }
  
  
  private static class Range extends AANode implements Cloneable, Comparable {
    public long start;
    public long end;

    @Override
    public String toString() {
      return "Range(" + this.start + "," + this.end + ")";
    }

    public long size() {
      return (isNull() ? 0 : this.end - this.start + 1); // since it is all inclusive
    }

    public boolean isNull() {
      return this.start > this.end;
    }

    public Range remove(long lid) {
      if (lid < this.start || lid > this.end) { throw new AssertionError("Ranges : Illegal value passed to remove : "
                                                                         + this + " remove called for : " + lid); }
      if (this.start == lid) {
        this.start++;
        return null;
      } else if (this.end == lid) {
        this.end--;
        return null;
      } else {
        Range newRange = new Range(lid + 1, this.end);
        this.end = lid - 1;
        return newRange;
      }
    }

    public void merge(Range other) {
      if (this.start == other.end + 1) {
        this.start = other.start;
      } else if (this.end == other.start - 1) {
        this.end = other.end;
      } else {
        throw new AssertionError("Ranges : Merge is called on non contiguous value : " + this + " and other Range is "
                                 + other);
      }
    }

    public boolean add(long lid) {
      if (lid == this.start - 1) {
        this.start--;
        return true;
      } else if (lid == this.end + 1) {
        this.end++;
        return true;
      } else if (lid >= this.start && lid <= this.end) {
        return false;
      } else {
        throw new AssertionError("Ranges : Add is called on non contiguous value : " + this + " but trying to add "
                                 + lid);
      }
    }

    public Range(long start, long end) {
      this.start = start;
      this.end = end;
    }

    @Override
    public Object clone() {
      return new Range(this.start, this.end);
    }

    public int compareTo(Object o) {
      if (o instanceof Range) {
        Range other = (Range) o;
        if (this.start < other.start) {
          return -1;
        } else if (this.start == other.start) {
          return 0;
        } else {
          return 1;
        }
      } else {
        long n = ((MyLong) o).longValue();
        if (this.end < n) {
          return -1;
        } else if (n < this.start) {
          return 1;
        } else {
          return 0;
        }
      }
    }

    @Override
    protected void swap(AANode other) {
      if (other instanceof Range) {
        Range r = (Range) other;
        long temp = this.start;
        this.start = r.start;
        r.start = temp;
        temp = this.end;
        this.end = r.end;
        r.end = temp;
      } else {
        throw new AssertionError("AATree can't contain both Ranges and other types : " + this + " other : " + other);
      }
    }

    @Override
    protected Comparable getElement() {
      return this;
    }

  }

  private static final class MyLong implements Comparable {

    final long number;

    public MyLong(long number) {
      this.number = number;
    }

    public long longValue() {
      return this.number;
    }

    public int compareTo(Object o) {
      if (o instanceof Range) {
        Range r = (Range) o;
        if (this.number < r.start) {
          return -1;
        } else if (this.number > r.end) {
          return 1;
        } else {
          return 0;
        }
      } else {
        long other = ((MyLong) o).longValue();
        if (this.number < other) {
          return -1;
        } else if (this.number > other) {
          return 1;
        } else {
          return 0;
        }
      }
    }

    @Override
    public String toString() {
      return "MyLong@" + System.identityHashCode(this) + "(" + this.number + ")";
    }

  }
}
