/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.util.AATreeSet.AbstractTreeNode;
import com.tc.util.AATreeSet.Node;

import java.io.IOException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class RangeObjectIDSet extends ObjectIDSetBase {

  public RangeObjectIDSet(final Collection c) {
    super();
    if (c instanceof RangeObjectIDSet) {
      final RangeObjectIDSet other = (RangeObjectIDSet) c;
      // fast way to clone
      this.size = other.size();
      for (final Iterator i = other.ranges.iterator(); i.hasNext();) {
        this.ranges.add((Range) ((Range) i.next()).clone());
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
  public Object deserializeFrom(final TCByteBufferInput in) throws IOException {
    if (this.size != 0) { throw new RuntimeException("deserialize dirty ObjectIDSet"); }
    int _size = in.readInt();
    this.size = _size;
    while (_size > 0) {
      final long start = in.readLong();
      final long end = in.readLong();
      final Range r = new Range(start, end);
      this.ranges.add(r);
      _size -= r.size();
    }
    return this;
  }

  @Override
  public void serializeTo(final TCByteBufferOutput out) {
    out.writeInt(this.size);
    for (final Iterator i = this.ranges.iterator(); i.hasNext();) {
      final Range r = (Range) i.next();
      out.writeLong(r.start);
      out.writeLong(r.end);
    }
  }

  /**
   * ignore find bug warning for non thread safe increment of a volatile variable The volatile varibale is being used
   * only for validation of no concurrent modification while iterating the set
   */
  @FindbugsSuppressWarnings("VO_VOLATILE_INCREMENT")
  @Override
  public boolean contains(final ObjectID id) {
    final long lid = id.toLong();
    return (this.ranges.find(new MyLong(lid)) != null);
  }

  @Override
  public boolean remove(final ObjectID id) {
    final long lid = id.toLong();

    final Range current = (Range) this.ranges.find(new MyLong(lid));
    if (current == null) {
      // Not found
      return false;
    }
    final Range newRange = current.remove(lid);
    if (newRange != null) {
      this.ranges.add(newRange);
    } else if (current.isNull()) {
      this.ranges.remove(current);
    }
    this.size--;
    this.modCount++;
    return true;
  }

  /**
   * ignore find bug warning for non thread safe increment of a volatile variable The volatile varibale is being used
   * only for validation of no concurrent modification while iterating the set
   */
  @FindbugsSuppressWarnings("VO_VOLATILE_INCREMENT")
  /**
   * Optimized addAll method if the other collection is a RangeObjectIDSet too. <br>
   * XXX: Use iterator for both the range sets as they give sorted sets. find() is costlier. XXX: More optimizations can
   * be done. refer BitObjectIDSet.addAll XXX: Add more tests for corner cases
   */
  public boolean addAll(final RangeObjectIDSet rangeObjectIDSet) {
    for (Iterator i = rangeObjectIDSet.ranges.iterator(); i.hasNext();) {
      Range rangeToAdd = (Range) i.next();
      if (rangeToAdd.size() <= 0) continue;
      Range startRange = (Range) this.ranges.find(new MyLong(rangeToAdd.start));
      Range endRange = (Range) this.ranges.find(new MyLong(rangeToAdd.end));

      if (startRange != null && endRange != null) {
        for (long l = startRange.end + 1; l < endRange.start; l++) {
          Range o = (Range) this.ranges.find(new MyLong(l));
          if (o != null) {
            this.ranges.remove(o);
            this.size -= o.size();
          }
        }
        this.size += (endRange.start - startRange.end - 1);
        startRange.end = endRange.end;
      } else if ((startRange != null) && (endRange == null)) {
        Range endNextRange = ((Range) this.ranges.find(new MyLong(rangeToAdd.end + 1)));
        if (endNextRange != null) {
          startRange.end = rangeToAdd.end;
          startRange.merge(endNextRange);
          this.ranges.remove(endNextRange);
        } else {
          startRange.end = rangeToAdd.end;
        }
        this.size += (rangeToAdd.end - startRange.end);
      } else if ((endRange != null) && (startRange == null)) {
        Range startPrevRange = ((Range) this.ranges.find(new MyLong(rangeToAdd.start - 1)));
        if (startPrevRange != null) {
          endRange.start = rangeToAdd.start;
          endRange.merge(startPrevRange);
          this.ranges.remove(startPrevRange);
        } else {
          endRange.start = rangeToAdd.start;
        }
        this.size += (endRange.start - rangeToAdd.end);
      } else if ((endRange == null) && (startRange == null)) {
        this.ranges.add((Range) rangeToAdd.clone());
        this.size += rangeToAdd.size();
      } else {
        // range add not needed
      }
      this.modCount++;
    }

    // XXX: validate and return
    return true;
  }

  /**
   * ignore find bug warning for non thread safe increment of a volatile variable The volatile varibale is being used
   * only for validation of no concurrent modification while iterating the set
   */
  @FindbugsSuppressWarnings("VO_VOLATILE_INCREMENT")
  @Override
  public boolean add(final ObjectID id) {
    final long lid = id.toLong();

    // Step 1 : Check if the previous number is present, if so add to the same Range.
    final Range prev = (Range) this.ranges.find(new MyLong(lid - 1));
    if (prev != null) {
      final boolean isAdded = prev.add(lid);
      if (isAdded) {
        final Range next = (Range) this.ranges.removeAndReturn((new MyLong(lid + 1)));
        if (next != null) {
          prev.merge(next);
        }
        this.size++;
        this.modCount++;
      }
      return isAdded;
    }

    // Step 2 : Check if the next number is present, if so add to the same Range.
    final Range next = (Range) this.ranges.find((new MyLong(lid + 1)));
    if (next != null) {
      final boolean isAdded = next.add(lid);
      if (isAdded) {
        this.size++;
        this.modCount++;
      }
      return isAdded;
    }

    // Step 3: Add a new range for just this number.
    final boolean isAdded = this.ranges.add(new Range(lid, lid));
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
    final Range min = (Range) this.ranges.first();
    return new ObjectID(min.start);
  }

  @Override
  public ObjectID last() {
    if (this.size == 0) { throw new NoSuchElementException(); }
    final Range max = (Range) this.ranges.last();
    return new ObjectID(max.end);
  }

  private class RangeObjectIDSetIterator implements Iterator {

    Iterator      nodes;
    private Range current;
    ObjectID      lastReturned;
    int           idx;
    int           expectedModCount;

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
      final ObjectID oid = new ObjectID(this.current.start + this.idx);
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
      this.nodes = RangeObjectIDSet.this.ranges.tailSet(new MyLong(this.lastReturned.toLong())).iterator();
      if (this.nodes.hasNext()) {
        this.current = (Range) this.nodes.next();
        this.idx = 0; // TODO:: verify ;; has to be
      } else {
        this.current = null;
      }
      this.lastReturned = null;
    }
  }

  private static class Range extends AbstractTreeNode<Comparable> implements Cloneable, Comparable<Comparable> {
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

    public Range remove(final long lid) {
      if (lid < this.start || lid > this.end) { throw new AssertionError("Ranges : Illegal value passed to remove : "
                                                                         + this + " remove called for : " + lid); }
      if (this.start == lid) {
        this.start++;
        return null;
      } else if (this.end == lid) {
        this.end--;
        return null;
      } else {
        final Range newRange = new Range(lid + 1, this.end);
        this.end = lid - 1;
        return newRange;
      }
    }

    public void merge(final Range other) {
      if (this.start == other.end + 1) {
        this.start = other.start;
      } else if (this.end == other.start - 1) {
        this.end = other.end;
      } else {
        throw new AssertionError("Ranges : Merge is called on non contiguous value : " + this + " and other Range is "
                                 + other);
      }
    }

    public boolean add(final long lid) {
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

    public Range(final long start, final long end) {
      this.start = start;
      this.end = end;
    }

    @Override
    public Object clone() {
      return new Range(this.start, this.end);
    }

    public int compareTo(final Comparable o) {
      if (o instanceof Range) {
        final Range other = (Range) o;
        if (this.start < other.start) {
          return -1;
        } else if (this.start == other.start) {
          return 0;
        } else {
          return 1;
        }
      } else {
        final long n = ((MyLong) o).longValue();
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
    public boolean equals(Object obj) {
      if (!(obj instanceof Range)) return false;
      Range r = (Range) obj;
      return (this.start == r.start) && (this.end == r.end);
    }

    public void swapPayload(final Node<Comparable> other) {
      if (other instanceof Range) {
        final Range r = (Range) other;
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

    public Comparable getPayload() {
      return this;
    }

  }

  private static final class MyLong implements Comparable {

    final long number;

    public MyLong(final long number) {
      this.number = number;
    }

    public long longValue() {
      return this.number;
    }

    public int compareTo(final Object o) {
      if (o instanceof Range) {
        final Range r = (Range) o;
        if (this.number < r.start) {
          return -1;
        } else if (this.number > r.end) {
          return 1;
        } else {
          return 0;
        }
      } else {
        final long other = ((MyLong) o).longValue();
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
