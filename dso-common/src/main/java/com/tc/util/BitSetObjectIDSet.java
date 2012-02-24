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
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class BitSetObjectIDSet extends ObjectIDSetBase {

  public BitSetObjectIDSet(final Collection c) {
    super();
    if (c instanceof BitSetObjectIDSet) {
      final BitSetObjectIDSet other = (BitSetObjectIDSet) c;
      // fast way to clone
      this.size = other.size();
      for (final Iterator<BitSet> i = other.ranges.iterator(); i.hasNext();) {
        this.ranges.add(new BitSet(i.next()));
      }
      return;
    } else {
      addAll(c);
    }
  }

  /**
   * Optimized addAll method if the other collection is a BitSetObjectIDSet too.<br>
   * <p>
   * Some Assumptions are <br>
   * 1. AATreeSetIterator iterates in sorted order <br>
   * 2. start is always in fixed multiple
   */
  public boolean addAll(final BitSetObjectIDSet o) {
    final int oldSize = this.size;
    final Iterator myRanges = this.ranges.iterator();
    final Iterator otherRanges = o.ranges.iterator();
    final ArrayList toAdd = new ArrayList();
    BitSet currentMine = null;
    while (otherRanges.hasNext()) {
      if (currentMine == null) {
        // First Iteration
        if (myRanges.hasNext()) {
          currentMine = (BitSet) myRanges.next();
        } else {
          // No ranges in this set, just clone and add and return
          copyAndAddAll(otherRanges);
          break;
        }
      }
      final BitSet nextOther = (BitSet) otherRanges.next();
      while (currentMine.start < nextOther.start && myRanges.hasNext()) {
        currentMine = (BitSet) myRanges.next();
      }
      if (currentMine.start == nextOther.start) {
        // Same range, can be merged
        final long sizeBefore = currentMine.size();
        currentMine.addAll(nextOther);
        this.size += currentMine.size() - sizeBefore;
      } else {
        // currentMine.start > nextOther.start || !myRanges.hasNext()
        toAdd.add(nextOther);
        if (currentMine.start < nextOther.start && !myRanges.hasNext()) {
          // No more ranges in this set, copy the rest directly saving a copy
          copyAndAddAll(otherRanges);
        }
      }
    }
    copyAndAddAll(toAdd.iterator());
    return (oldSize < this.size);
  }

  private void copyAndAddAll(final Iterator<BitSet> i) {
    for (; i.hasNext();) {
      final BitSet copied = new BitSet(i.next());
      this.size += copied.size();
      boolean added = this.ranges.add(copied);
      if (!added) { throw new AssertionError("cloned : " + copied + " is not added to this set : " + this); }
    }
  }

  public BitSetObjectIDSet() {
    super();
  }

  @Override
  /**
   * ignore find bug warning for non thread safe increment of a volatile variable
   * The volatile varibale is being used only for validation of no concurrent modification while iterating the set
   */
  @FindbugsSuppressWarnings("VO_VOLATILE_INCREMENT")
  public boolean add(final ObjectID id) {
    final long lid = id.toLong();

    // need to handle -ve ids
    final long start = calculateStart(lid);
    int nextRangeMaskbit = 0;
    if (lid < 0) {
      nextRangeMaskbit = (int) (BitSet.RANGE_SIZE - ((-lid) % BitSet.RANGE_SIZE));
    } else {
      nextRangeMaskbit = (int) (lid % BitSet.RANGE_SIZE);
    }

    // Step 1 : Check if number can be contained in any of the range, if so add to the same Range.
    final BitSet prev = (BitSet) this.ranges.find(new BitSet(start, 0));
    if (prev != null) {
      final boolean isAdded = prev.add(lid);
      if (isAdded) {
        this.size++;
        this.modCount++;
      }
      return isAdded;
    }

    // Step 2: Add a new range for just this number.
    final long nextRange = 1L << nextRangeMaskbit;
    final BitSet newRange = new BitSet(start, nextRange);
    final boolean isAdded = this.ranges.add(newRange);
    if (isAdded) {
      this.size++;
      this.modCount++;
    }
    return isAdded;
  }

  /**
   * ignore find bug warning for non thread safe increment of a volatile variable The volatile varibale is being used
   * only for validation of no concurrent modification while iterating the set
   */
  @FindbugsSuppressWarnings("VO_VOLATILE_INCREMENT")
  @Override
  public boolean remove(final ObjectID id) {
    final long lid = id.toLong();

    final long start = calculateStart(lid);

    final BitSet current = (BitSet) this.ranges.find(new BitSet(start, 0));
    if (current == null) {
      // Not found
      return false;
    }
    if (current.remove(lid)) {
      if (current.isEmpty()) {
        this.ranges.remove(current);
      }
      this.size--;
      this.modCount++;
      return true;
    }
    return false;
  }

  @Override
  public boolean contains(final ObjectID id) {
    final long lid = id.toLong();
    final long start = calculateStart(lid);
    final BitSet r = (BitSet) this.ranges.find(new BitSet(start, 0));
    if (r == null) {
      return false;
    } else {
      return isPresent(lid, r);
    }
  }

  @Override
  public boolean contains(final Object o) {
    if (o instanceof ObjectID) {
      return contains((ObjectID) o);
    } else {
      return false;
    }
  }

  private boolean isPresent(final long lid, final BitSet r) {
    final long maskBit = 1L << (int) (lid - r.start);
    if ((r.nextLongs & maskBit) != 0) { return true; }
    return false;
  }

  @Override
  public Object deserializeFrom(final TCByteBufferInput in) throws IOException {
    if (this.size != 0) { throw new RuntimeException("deserialize dirty ObjectIDSet"); }
    int _size = in.readInt();
    this.size = _size;
    while (_size > 0) {
      final long start = in.readLong();
      final long nextRanges = in.readLong();
      final BitSet r = new BitSet(start, nextRanges);
      this.ranges.add(r);
      _size -= r.size();
    }
    return this;
  }

  @Override
  public void serializeTo(final TCByteBufferOutput out) {
    out.writeInt(this.size);
    for (final Iterator i = this.ranges.iterator(); i.hasNext();) {
      final BitSet r = (BitSet) i.next();
      out.writeLong(r.start);
      out.writeLong(r.nextLongs);
    }
  }

  @Override
  public Iterator iterator() {
    return new BitSetObjectIDSetIterator();
  }

  @Override
  public ObjectID first() {
    if (this.size == 0) { throw new NoSuchElementException(); }
    final BitSet min = (BitSet) this.ranges.first();
    return new ObjectID(min.first());
  }

  @Override
  public ObjectID last() {
    if (this.size == 0) { throw new NoSuchElementException(); }
    final BitSet max = (BitSet) this.ranges.last();
    return new ObjectID(max.last());
  }

  public static long calculateStart(final long lid) {
    if (lid < 0) {
      return (-BitSet.RANGE_SIZE + ((lid + 1) / BitSet.RANGE_SIZE) * BitSet.RANGE_SIZE);
    } else {
      return (lid - (lid % BitSet.RANGE_SIZE));
    }
  }

  private class BitSetObjectIDSetIterator implements Iterator {

    private Iterator nodes;
    private BitSet   current;
    private BitSet   next;
    private ObjectID lastReturned;
    private int      idx;
    private int      expectedModCount;

    public BitSetObjectIDSetIterator() {
      this.nodes = BitSetObjectIDSet.this.ranges.iterator();
      this.expectedModCount = BitSetObjectIDSet.this.modCount;
      this.idx = 0;
      if (this.nodes.hasNext()) {
        this.current = (BitSet) this.nodes.next();
      }
      this.next = (BitSet) (this.nodes.hasNext() ? this.nodes.next() : null);
    }

    public boolean hasNext() {
      return (this.next != null) || (this.current != null && !isPointingToLast());
    }

    private boolean isPointingToLast() {
      if (this.current.last() >= this.current.start + this.idx) { return false; }
      return true;
    }

    public Object next() {
      if (this.current == null) { throw new NoSuchElementException(); }
      if (this.expectedModCount != BitSetObjectIDSet.this.modCount) { throw new ConcurrentModificationException(); }
      moveToNextIndex();
      final ObjectID oid = new ObjectID(this.current.start + this.idx);
      this.idx++;
      return (this.lastReturned = oid);
    }

    private void moveToNextIndex() {
      if (this.current == null) {
        this.idx = 0;
        return;
      }

      long maskBit = 1L << this.idx;
      while (((this.current.nextLongs & maskBit) == 0) && this.idx < BitSet.RANGE_SIZE) {
        this.idx++;
        maskBit = 1L << this.idx;
      }
      if (this.idx >= BitSet.RANGE_SIZE) {
        moveToNextGroup();
      }
    }

    private void moveToNextGroup() {
      this.idx = 0;
      if (this.next != null) {
        this.current = this.next;
        moveToNextIndex();
        this.next = (BitSet) (this.nodes.hasNext() ? this.nodes.next() : null);
      } else {
        this.current = null;
      }
    }

    /**
     * ignore find bug warning for non thread safe increment of a volatile variable The volatile varibale is being used
     * only for validation of no concurrent modification while iterating the set
     */
    @FindbugsSuppressWarnings("VO_VOLATILE_INCREMENT")
    public void remove() {
      if (this.lastReturned == null) { throw new IllegalStateException(); }
      if (this.expectedModCount != BitSetObjectIDSet.this.modCount) { throw new ConcurrentModificationException(); }

      final long oid = this.lastReturned.toLong();
      final long lastElement = this.current.last();
      this.current.remove(this.lastReturned.toLong());
      BitSetObjectIDSet.this.size--;
      BitSetObjectIDSet.this.modCount++;

      if (!this.current.isEmpty()) {
        if (lastElement == this.lastReturned.toLong()) {
          // if it was the highest element in the range set then move the pointer to next
          this.current = this.next;
          this.next = (BitSet) (this.nodes.hasNext() ? this.nodes.next() : null);
          this.idx = 0;
        }
      } else {
        // if all the elements got removed because of this removal then remove the node
        // and create tailset iterator
        BitSetObjectIDSet.this.ranges.remove(this.current);
        this.nodes = BitSetObjectIDSet.this.ranges.tailSet(new BitSet(calculateStart(oid), 0)).iterator();
        // this.nodes = BitSetObjectIDSet.this.ranges.tailSetIterator(this.next);
        this.idx = 0;
        this.current = (BitSet) (this.nodes.hasNext() ? this.nodes.next() : null);
        this.next = (BitSet) (this.nodes.hasNext() ? this.nodes.next() : null);
      }

      if (this.current != null) {
        moveToNextIndex();
      } else {
        this.idx = 0;
      }

      this.expectedModCount = BitSetObjectIDSet.this.modCount;
      this.lastReturned = null;
    }

  }

  /**
   * Ranges store the elements stored in the tree. The range is inclusive.
   */
  public static final class BitSet extends AbstractTreeNode<BitSet> implements Comparable<BitSet> {
    private long            start;
    private long            nextLongs  = 0;
    public static final int RANGE_SIZE = 64;

    public BitSet(final long start, final long nextRanges) {
      this.start = start;
      this.nextLongs = nextRanges;
    }

    public BitSet(BitSet copyThis) {
      this(copyThis.start, copyThis.nextLongs);
    }

    public void addAll(final BitSet other) {
      if (this.start != other.start) { throw new AssertionError("Ranges : Start is not the same. mine : " + this.start
                                                                + " other : " + other.start); }
      this.nextLongs |= other.nextLongs;
    }

    @Override
    public String toString() {
      return "Range(" + this.start + "," + Long.toBinaryString(this.nextLongs) + ")";
    }

    public boolean isEmpty() {
      return this.nextLongs == 0;
    }

    public long size() {
      return (Long.bitCount(this.nextLongs)); // since it is all inclusive
    }

    public boolean remove(final long lid) {
      if (lid < this.start || lid >= this.start + RANGE_SIZE) { throw new AssertionError(
                                                                                         "Ranges : Illegal value passed to remove : "
                                                                                             + this
                                                                                             + " remove called for : "
                                                                                             + lid); }
      long maskBits = 1L << (int) (lid - this.start);
      maskBits &= this.nextLongs;
      this.nextLongs ^= maskBits;
      return (maskBits != 0);
    }

    public boolean add(final long lid) {
      if (lid < this.start || lid >= this.start + RANGE_SIZE) { throw new AssertionError(
                                                                                         "Ranges : Illegal value passed to add : "
                                                                                             + this
                                                                                             + " add called for : "
                                                                                             + lid); }
      final long maskBits = 1L << (int) (lid - this.start);
      if ((this.nextLongs & maskBits) == 0) {
        this.nextLongs = this.nextLongs | maskBits;
        return true;
      }
      return false;
    }

    /**
     * while comparing we only care about start since that tells us the starting point of the sets of integer in this
     * bit set
     */
    public int compareTo(final BitSet o) {
      final BitSet other = o;
      if (this.start < other.start) {
        return -1;
      } else if (this.start == other.start) {
        return 0;
      } else {
        return 1;
      }
    }

    /**
     * this returns true if start and nextLongs both are equal, Note that compareTo does not hold the same contract
     */
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof BitSet)) return false;
      BitSet o = (BitSet) obj;
      return (this.start == o.start) && (this.nextLongs == o.nextLongs);
    }

    public void swapPayload(final Node<BitSet> other) {
      if (other instanceof BitSet) {
        final BitSet r = (BitSet) other;
        long temp = this.start;
        this.start = r.start;
        r.start = temp;
        temp = this.nextLongs;
        this.nextLongs = r.nextLongs;
        r.nextLongs = temp;
      } else {
        throw new AssertionError("AATree can't contain both Ranges and other types : " + this + " other : " + other);
      }
    }

    public BitSet getPayload() {
      return this;
    }

    public long first() {
      if (this.nextLongs == 0) { throw new NoSuchElementException(); }
      return this.start + Long.numberOfTrailingZeros(this.nextLongs);
    }

    public long last() {
      if (this.nextLongs == 0) { throw new NoSuchElementException(); }
      return this.start + BitSet.RANGE_SIZE - 1 - Long.numberOfLeadingZeros(this.nextLongs);
    }
  }

}
