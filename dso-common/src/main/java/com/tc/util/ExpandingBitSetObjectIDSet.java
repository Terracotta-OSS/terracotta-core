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
package com.tc.util;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.ObjectID;
import com.tc.util.AATreeSet.AbstractTreeNode;
import com.tc.util.AATreeSet.Node;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ExpandingBitSetObjectIDSet extends ObjectIDSet {

  private int size;
  private AATreeSet<BitSet> ranges = new AATreeSet<BitSet>();
  private volatile int modCount;

  public ExpandingBitSetObjectIDSet(final Collection c) {
    // TODO: This is busted for some reason...
//    if (c instanceof ObjectIDSet) {
//      for (Range range : ((ObjectIDSet)c).ranges()) {
//        insertRange(range);
//      }
//    } else {
      addAll(c);
//    }
  }

  public ExpandingBitSetObjectIDSet() {
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

    // Step 1 : Check if number can be contained in any of the range, if so add to the same Range.
    BitSet probe = new BitSet(lid);
    final BitSet prev = ranges.find(probe);
    if (prev != null) {
      final boolean isAdded = prev.add(lid);
      if (isAdded) {
        this.size++;
        this.modCount++;
      }
      return isAdded;
    }

    // Step 2: Check if the left neighbor exists for merging
    final BitSet left = ranges.find(probe.leftNeighbor());
    final BitSet right = ranges.find(probe.rightNeighbor());
    if (left != null && left.merge(probe)) {
      // Left and right cannot be equal because in order to get to this step, the probe must have landed in a gap meaning
      // its neighbors can't be equal (otherwise there would be no gap)
      if (right != null && left.merge(right)) {
        // 3 way merge successful, so remove the right neighbor
        ranges.remove(right);
      }
      size++;
      modCount++;
      return true;
    } else if (right != null && probe.merge(right)) {
      // No need to check for a 3 way merge here, either left doesn't exist or isn't mergeable
      // Right's been merged into the new BitSet, so remove it from the ranges before adding the new one.
      ranges.remove(right);
      ranges.add(probe);
      size++;
      modCount++;
      return true;
    }

    // Step 3: Add a new range for just this number.
    final boolean isAdded = ranges.add(probe);
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
  public boolean remove(final Object o) {
    if (!(o instanceof ObjectID)) {
      return false;
    }
    ObjectID id = (ObjectID) o;
    final long lid = id.toLong();

    final BitSet current = this.ranges.find(new BitSet(lid));
    if (current == null) {
      // Not found
      return false;
    }
    if (current.remove(lid)) {
      BitSet split = current.split(lid);
      if (split != null) {
        ranges.add(split);
      }
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
  public boolean contains(final Object o) {
    if (!(o instanceof ObjectID)) {
      return false;
    }
    ObjectID id = (ObjectID) o;
    final long lid = id.toLong();
    final BitSet r = this.ranges.find(new BitSet(lid));
    return r != null && r.contains(lid);
  }

  @Override
  public Iterator<ObjectID> iterator() {
    return new ObjectIDSetIterator();
  }

  @Override
  public ObjectID first() {
    if (this.size == 0) { throw new NoSuchElementException(); }
    final BitSet min = this.ranges.first();
    return new ObjectID(min.first());
  }

  @Override
  public ObjectID last() {
    if (this.size == 0) { throw new NoSuchElementException(); }
    final BitSet max = this.ranges.last();
    return new ObjectID(max.last());
  }

  private class ObjectIDSetIterator implements Iterator<ObjectID> {

    private Iterator<BitSet> nodes;

    private Iterator<Long> currentIterator;
    private BitSet current;

    private Iterator<Long> lastReturnedIterator;
    private BitSet lastReturnedBitSet;
    private long lastReturnedValue;

    private int      expectedModCount;

    public ObjectIDSetIterator() {
      this.nodes = ExpandingBitSetObjectIDSet.this.ranges.iterator();
      this.expectedModCount = ExpandingBitSetObjectIDSet.this.modCount;
      advance();
    }

    @Override
    public boolean hasNext() {
      checkModCount();
      advance();
      return currentIterator != null && currentIterator.hasNext();
    }

    private void advance() {
      if (currentIterator == null || !currentIterator.hasNext()) {
        if (nodes.hasNext()) {
          BitSet bitSet = nodes.next();
          if (bitSet.isEmpty()) {
            throw new AssertionError();
          }
          current = bitSet;
          currentIterator = current.iterator();
        }
      }
    }

    @Override
    public ObjectID next() {
      checkModCount();
      advance();
      lastReturnedIterator = currentIterator;
      lastReturnedBitSet = current;
      lastReturnedValue = currentIterator.next();
      return new ObjectID(lastReturnedValue);
    }

    /**
     * ignore find bug warning for non thread safe increment of a volatile variable The volatile varibale is being used
     * only for validation of no concurrent modification while iterating the set
     */
    @Override
    @FindbugsSuppressWarnings("VO_VOLATILE_INCREMENT")
    public void remove() {
      if (lastReturnedIterator == null) {
        throw new IllegalStateException();
      }
      checkModCount();

      lastReturnedIterator.remove();

      ExpandingBitSetObjectIDSet.this.size--;
      ExpandingBitSetObjectIDSet.this.modCount++;

      BitSet split = lastReturnedBitSet.split(lastReturnedValue);

      if (split != null) {
        if (!ranges.add(split)) {
          throw new AssertionError();
        }
        // Remove the left split if it's empty
        if (lastReturnedBitSet.isEmpty() && !ranges.remove(lastReturnedBitSet)) {
          throw new RuntimeException();
        }
        // We got a right split, that means the returned split _must_ be the next in line to be iterated.
        nodes = ranges.tailSet(split).iterator();
        current = null;
        currentIterator = null;
      } else if (lastReturnedBitSet.isEmpty()) {
        // The whole BitSet is empty, remove it.
        if (!ranges.remove(lastReturnedBitSet)) {
          throw new RuntimeException();
        }
        // This will pick up whatever is next.
        nodes = ranges.tailSet(lastReturnedBitSet).iterator();
        current = null;
        currentIterator = null;
      }

      expectedModCount = ExpandingBitSetObjectIDSet.this.modCount;
    }

    private void checkModCount() {
      if (this.expectedModCount != ExpandingBitSetObjectIDSet.this.modCount) { throw new ConcurrentModificationException(); }

    }
  }

  /**
   * Ranges store the elements stored in the tree. The range is inclusive.
   */
  static final class BitSet extends AbstractTreeNode<BitSet> implements Comparable<BitSet>, TCSerializable, Iterable<Long>, Range {
    private static final int MIN_SIZE = 16;
    // Needs to be a multiple of MIN_SIZE
    private static final int MAX_SIZE = 8192;
    private static final int SPLIT_THRESHOLD = 16;

    private long            start;
    private long[]          nextLongs;

    BitSet(final long initial) {
      start = initial & ~((Long.SIZE * MIN_SIZE) - 1);
      nextLongs = new long[MIN_SIZE];
      add(initial);
    }

    BitSet(final long start, final long[] nextRanges) {
      this.start = start;
      this.nextLongs = nextRanges;
    }

    BitSet(BitSet copyThis) {
      this(copyThis.start, Arrays.copyOf(copyThis.nextLongs, copyThis.nextLongs.length));
    }

    @Override
    public void serializeTo(final TCByteBufferOutput serialOutput) {
      serialOutput.writeLong(start);
      serialOutput.writeInt(nextLongs.length);
      for (long l : nextLongs) {
        serialOutput.writeLong(l);
      }
    }

    @Override
    public Object deserializeFrom(final TCByteBufferInput serialInput) throws IOException {
      start = serialInput.readLong();
      nextLongs = new long[serialInput.readInt()];
      for (int i = 0; i < nextLongs.length; i++) {
        nextLongs[i] = serialInput.readLong();
      }
      return this;
    }

    public void addAll(final BitSet other) {
      if (this.start != other.start) { throw new AssertionError("Ranges : Start is not the same. mine : " + this.start
                                                                + " other : " + other.start); }
      if (nextLongs.length != other.nextLongs.length) {
        for (int i = 0; i < nextLongs.length; i++) {
          nextLongs[i] |= other.nextLongs[i];
        }
      }
    }

    boolean merge(BitSet right) {
      if (max() + 1 != right.start) {
        throw new IllegalArgumentException();
      }
      if (nextLongs.length + right.nextLongs.length >= MAX_SIZE) {
        return false;
      }
      long[] newNextLongs = new long[nextLongs.length + right.nextLongs.length];
      System.arraycopy(nextLongs, 0, newNextLongs, 0, nextLongs.length);
      System.arraycopy(right.nextLongs, 0, newNextLongs, nextLongs.length, right.nextLongs.length);
      nextLongs = newNextLongs;
      return true;
    }

    BitSet split(long hint) {
      BitSet split = null;
      if (!isEmpty() && nextLongs[arrayIndex(hint)] == 0) {
        // Identify the run of empty blocks
        int left = arrayIndex(hint);
        while (left > 0) {
          if (nextLongs[left] != 0) {
            break;
          } else {
            left--;
          }
        }
        // Align left to MIN_SIZE boundaries
        left += (MIN_SIZE - ((left + 1) % MIN_SIZE));
        int right = arrayIndex(hint);
        while (right < nextLongs.length) {
          if (nextLongs[right] != 0) {
            break;
          } else {
            right++;
          }
        }
        // Align right to MIN_SIZE boundaries
        right -= (right % MIN_SIZE);
        if (right - left >= SPLIT_THRESHOLD) {
          if (right < nextLongs.length) {
            long splitStart = start + right * Long.SIZE;
            long[] splitLongs = new long[nextLongs.length - right];
            System.arraycopy(nextLongs, right, splitLongs, 0, splitLongs.length);
            split = new BitSet(splitStart, splitLongs);
            if (split.isEmpty()) {
              throw new AssertionError();
            }
          }
          long[] temp = new long[left + 1];
          System.arraycopy(nextLongs, 0, temp, 0, temp.length);
          nextLongs = temp;
        }
      }
      return split;
    }

    int capacity() {
      return nextLongs.length * Long.SIZE;
    }

    BitSet leftNeighbor() {
      return new BitSet(start - 1);
    }

    BitSet rightNeighbor() {
      return new BitSet(max() + 1);
    }

    @Override
    public String toString() {
      return "Range(" + this.start + "," + Arrays.toString(nextLongs) + ")";
    }

    public boolean isEmpty() {
      for (long l : nextLongs) {
        if (l != 0) {
          return false;
        }
      }
      return true;
    }

    boolean contains(long lid) {
      if (lid < start || lid > start + nextLongs.length * Long.SIZE) {
        return false;
      }
      return (nextLongs[arrayIndex(lid)] & maskBit(lid)) != 0;
    }

    public long size() {
      long size = 0;
      for (long l : nextLongs) {
        size += Long.bitCount(l);
      }
      return size;
    }

    public boolean remove(final long lid) {
      if (lid < this.start || lid >= this.start + nextLongs.length * Long.SIZE) { throw new AssertionError(
                                                                                         "Ranges : Illegal value passed to remove : "
                                                                                             + this
                                                                                             + " remove called for : "
                                                                                             + lid); }
      int arrayOffset = arrayIndex(lid);
      long maskBit = maskBit(lid) & nextLongs[arrayOffset];
      nextLongs[arrayOffset] ^= maskBit;
      return (maskBit != 0);
    }

    public boolean add(final long lid) {
      if (lid < this.start || lid >= this.start + nextLongs.length * Long.SIZE) { throw new AssertionError(
                                                                                         "Ranges : Illegal value passed to add : "
                                                                                             + this
                                                                                             + " add called for : "
                                                                                             + lid); }
      int arrayIndex = arrayIndex(lid);
      long maskBit = maskBit(lid);
      if ((nextLongs[arrayIndex] & maskBit) == 0) {
        nextLongs[arrayIndex] |= maskBit;
        return true;
      }
      return false;
    }

    private int offset(long lid) {
      return (int) (lid - start);
    }

    private int arrayIndex(long lid) {
      return offset(lid) >> Long.numberOfTrailingZeros(Long.SIZE);
    }

    private long maskBit(long lid) {
      return (1L << (offset(lid) & (Long.SIZE - 1)));
    }

    @Override
    public int compareTo(final BitSet o) {
      if (start <= o.start) {
        if (max() >= o.max()) {
          // Return 0 if this bitset is a superset of o.
          return 0;
        } else {
          return -1;
        }
      } else {
        return 1;
      }
    }

    private long max() {
      return start + nextLongs.length * Long.SIZE - 1;
    }

    /**
     * this returns true if start and nextLongs both are equal, Note that compareTo does not hold the same contract
     */
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof BitSet)) return false;
      BitSet o = (BitSet) obj;
      return (start == o.start) && Arrays.equals(nextLongs, o.nextLongs);
    }

    @Override
    public int hashCode() {
      return (int) (start * 31 + Arrays.hashCode(nextLongs));
    }

    @Override
    public void swapPayload(final Node<BitSet> other) {
      if (other instanceof BitSet) {
        final BitSet r = (BitSet)other;
        long temp = start;
        start = r.start;
        r.start = temp;
        long[] nextLongsTemp = this.nextLongs;
        nextLongs = r.nextLongs;
        r.nextLongs = nextLongsTemp;
      } else {
        throw new AssertionError("AATree can't contain both Ranges and other types : " + this + " other : " + other);
      }
    }

    @Override
    public BitSet getPayload() {
      return this;
    }

    public long first() {
      if (isEmpty()) { throw new NoSuchElementException(); }
      long first = start;
      for (long l : nextLongs) {
        if (l != 0) {
          return first + Long.numberOfTrailingZeros(l);
        } else {
          first += Long.SIZE;
        }
      }
      throw new AssertionError();
    }

    public long last() {
      if (isEmpty()) { throw new NoSuchElementException(); }
      long last = max();
      for (int i = nextLongs.length - 1; i >= 0; i--) {
        if (nextLongs[i] != 0) {
          return last - Long.numberOfLeadingZeros(nextLongs[i]);
        } else {
          last -= Long.SIZE;
        }
      }
      throw new AssertionError();
    }

    @Override
    public Iterator<Long> iterator() {
      return new BitSetIterator();
    }

    @Override
    public long getStart() {
      return start;
    }

    @Override
    public long[] getBitmap() {
      return nextLongs;
    }

    private class BitSetIterator implements Iterator<Long> {
      long prev = max() + 1;
      long next = start - 1;

      BitSetIterator() {
        advance();
      }

      @Override
      public boolean hasNext() {
        return next <= max();
      }

      @Override
      public Long next() {
        if (next > max()) {
          throw new NoSuchElementException();
        }
        prev = next;
        advance();
        return prev;
      }

      private void advance() {
        while (++next <= max() && !contains(next)) {
          // Nothing to do
        }
      }

      @Override
      public void remove() {
        if (prev > max() || prev < start || !BitSet.this.remove(prev)) {
          throw new NoSuchElementException();
        }
      }
    }
  }

  @Override
  protected void insertRange(final Range range) {
    BitSet bitSet = new BitSet(range.getStart(), range.getBitmap());
    if (!bitSet.isEmpty()) {
      size += bitSet.size();
      //TODO: This is not really "expanding". Refactor the logic out of add that does the expanding to make this work
      ranges.add(bitSet);
    }
  }

  @Override
  protected Collection<? extends Range> ranges() {
    return ranges;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ExpandingBitSetObjectIDSet{");
    sb.append("size=").append(size);
    sb.append(", ranges=").append(ranges);
    sb.append(", modCount=").append(modCount);
    sb.append('}');
    return sb.toString();
  }
}
