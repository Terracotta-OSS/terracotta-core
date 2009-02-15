/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.ObjectID;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.io.IOException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * This class was build in an attempt to store a large set of ObjectIDs compressed in memory while giving the same
 * characteristic of HashSet in terms of performance.
 * <p>
 * This is version 2 of the class, the older one version 1 has several shortcomings. Mainly the performance of adds and
 * removes when the ObjectIDs are non-contiguous.
 * <p>
 * This one uses a balanced tree internally to store ranges instead of an ArrayList
 */
public class ObjectIDSet extends AbstractSet implements SortedSet, PrettyPrintable, TCSerializable {

  /**
   * modCount - number of times this HashMap has been structurally modified Structural modifications are those that
   * change the number of mappings in the HashMap or otherwise modify its internal structure (e.g., rehash). This field
   * is used to make iterators on Collection-views of the HashMap fail-fast. (See ConcurrentModificationException).
   */
  private transient volatile int modCount;
  private final AATreeSet        ranges;
  private int                    size = 0;

  public ObjectIDSet() {
    this.ranges = new AATreeSet();
  }

  public ObjectIDSet(Collection c) {
    this();
    if (c instanceof ObjectIDSet) {
      ObjectIDSet other = (ObjectIDSet) c;
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

  public void serializeTo(TCByteBufferOutput out) {
    out.writeInt(this.size);
    for (Iterator i = this.ranges.iterator(); i.hasNext();) {
      Range r = (Range) i.next();
      out.writeLong(r.start);
      out.writeLong(r.end);
    }
  }

  @Override
  public Iterator iterator() {
    return new ObjectIDSetIterator();
  }

  @Override
  public int size() {
    return this.size;
  }

  public boolean contains(ObjectID id) {
    long lid = id.toLong();
    return (this.ranges.find(new MyLong(lid)) != null);
  }

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
  public String toString() {
    if (size() <= 10) { return toVerboseString(); }

    StringBuffer sb = new StringBuffer("ObjectIDSet " + getCompressionDetails() + "[");
    for (Iterator i = this.ranges.iterator(); i.hasNext();) {
      sb.append(' ').append(i.next());
    }
    return sb.append(']').toString();
  }

  public String toVerboseString() {

    StringBuffer sb = new StringBuffer("ObjectIDSet [ ");
    for (Iterator<ObjectID> iter = iterator(); iter.hasNext();) {
      sb.append(iter.next());
      if (iter.hasNext()) {
        sb.append(", ");
      }
    }
    sb.append(" ]");

    return sb.toString();
  }

  public String toShortString() {
    StringBuffer sb = new StringBuffer("ObjectIDSet " + getCompressionDetails() + "[");
    sb.append(" size  = ").append(this.size);
    return sb.append(']').toString();
  }

  private String getCompressionDetails() {
    return "{ (oids:ranges) = " + this.size + ":" + this.ranges.size() + " , compression ratio = "
           + getCompressionRatio() + " } ";
  }

  // Range contains two longs instead of 1 long in ObjectID
  private float getCompressionRatio() {
    return (this.ranges.size() == 0 ? 1.0f : (this.size / (this.ranges.size() * 2.0f)));
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(toShortString());
    return out;
  }

  /**
   * Ranges store the elements stored in the tree. The range is inclusive.
   */
  private static class Range implements Cloneable, Comparable {
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
  }

  // This class is used as a key for lookup.
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

  private class ObjectIDSetIterator implements Iterator {

    Iterator nodes;
    Range    current;
    ObjectID lastReturned;
    int      idx;
    int      expectedModCount;

    public ObjectIDSetIterator() {
      this.nodes = ObjectIDSet.this.ranges.iterator();
      this.expectedModCount = ObjectIDSet.this.modCount;
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
      if (this.expectedModCount != ObjectIDSet.this.modCount) { throw new ConcurrentModificationException(); }
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
      if (this.expectedModCount != ObjectIDSet.this.modCount) { throw new ConcurrentModificationException(); }
      ObjectIDSet.this.remove(this.lastReturned);
      this.expectedModCount = ObjectIDSet.this.modCount;
      this.nodes = ObjectIDSet.this.ranges.tailSetIterator(new MyLong(this.lastReturned.toLong()));
      if (this.nodes.hasNext()) {
        this.current = (Range) this.nodes.next();
        this.idx = 0; // TODO:: verify ;; has to be
      } else {
        this.current = null;
      }
      this.lastReturned = null;
    }
  }

  /**
   * Even though the iterator now supports remove() we are still sticking with this implementation since it seems faster
   * than the calling iterator.remove() since the tree re-balances and we create new tail iterators on every remove
   * which seems costly.
   * 
   * @see ObjectIDSetTest.testRemoveAll()
   */
  @Override
  public boolean removeAll(Collection c) {
    boolean modified = false;
    if (size() > c.size()) {
      for (Iterator i = c.iterator(); i.hasNext();) {
        modified |= remove(i.next());
      }
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

  @Override
  public boolean contains(Object o) {
    if (o instanceof ObjectID) {
      return contains((ObjectID) o);
    } else {
      return false;
    }
  }

  @Override
  public boolean add(Object arg0) {
    return add((ObjectID) arg0);
  }

  @Override
  public boolean remove(Object o) {
    if (o instanceof ObjectID) {
      return remove((ObjectID) o);
    } else {
      return false;
    }
  }

  @Override
  public void clear() {
    this.size = 0;
    this.modCount++;
    this.ranges.clear();
  }

  // =======================SortedSet Interface Methods==================================

  public Comparator comparator() {
    return null;
  }

  public Object first() {
    if (this.size == 0) { throw new NoSuchElementException(); }
    Range min = (Range) this.ranges.findMin();
    return new ObjectID(min.start);
  }

  public Object last() {
    if (this.size == 0) { throw new NoSuchElementException(); }
    Range max = (Range) this.ranges.findMax();
    return new ObjectID(max.end);
  }

  public SortedSet headSet(Object arg0) {
    throw new UnsupportedOperationException();
  }

  public SortedSet subSet(Object arg0, Object arg1) {
    throw new UnsupportedOperationException();
  }

  public SortedSet tailSet(Object arg0) {
    throw new UnsupportedOperationException();
  }

  // ======================= UnmodifiableobjectIDSet ==================================

  public static ObjectIDSet unmodifiableObjectIDSet(ObjectIDSet s) {
    return new UnmodifiableObjectIDSet(s);
  }

  static class UnmodifiableObjectIDSet extends ObjectIDSet {
    final ObjectIDSet s;

    UnmodifiableObjectIDSet(ObjectIDSet s) {
      this.s = s;
    }

    @Override
    public Object deserializeFrom(TCByteBufferInput in) throws IOException {
      return this.s.deserializeFrom(in);
    }

    @Override
    public void serializeTo(TCByteBufferOutput out) {
      this.s.serializeTo(out);
    }

    @Override
    public boolean equals(Object o) {
      return o == this || this.s.equals(o);
    }

    @Override
    public int hashCode() {
      return this.s.hashCode();
    }

    @Override
    public int size() {
      return this.s.size();
    }

    @Override
    public boolean isEmpty() {
      return this.s.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return this.s.contains(o);
    }

    @Override
    public Object[] toArray() {
      return this.s.toArray();
    }

    @Override
    public String toString() {
      return this.s.toString();
    }

    @Override
    public Iterator iterator() {
      return new Iterator() {
        Iterator i = UnmodifiableObjectIDSet.this.s.iterator();

        public boolean hasNext() {
          return this.i.hasNext();
        }

        public Object next() {
          return this.i.next();
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public boolean add(Object e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection coll) {
      return this.s.containsAll(coll);
    }

    @Override
    public boolean addAll(Collection coll) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection coll) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection coll) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toVerboseString() {
      return this.s.toVerboseString();
    }

    @Override
    public String toShortString() {
      return this.s.toShortString();
    }

    @Override
    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      return this.s.prettyPrint(out);
    }
  }

}
