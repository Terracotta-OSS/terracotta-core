package com.tc.util;

import com.tc.object.ObjectID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author tim
 */
public class BasicObjectIDSet extends ObjectIDSet {
  private final List<Range> ranges = new ArrayList<Range>();
  private int size = 0;

  public BasicObjectIDSet() {
    //
  }
  
  public BasicObjectIDSet(String magic, long ... ids) {
    if (!"ImDoingTesting".equals(magic)) {
      throw new UnsupportedOperationException("This constructor is for testing only");
    }
    Arrays.sort(ids);
    for (long id : ids) {
      insertRange(new BasicRange(id, new long[] {1L}));
    }
  }
  
  @Override
  public Iterator<ObjectID> iterator() {
    return new OIDIterator();
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  protected void insertRange(final Range range) {
    for (Range smallRange : fragment(range)) {
      doRangeInsert(smallRange);
    }
  }

  private void doRangeInsert(final Range range) {
    if (range.getBitmap().length != 1) {
      throw new IllegalArgumentException("Wrong range size for range " + range);
    }
    // Try the end first, it should be inserting in order anyways.
    if (ranges.isEmpty() || RANGE_COMPARATOR.compare(ranges.get(ranges.size() - 1), range) < 0) {
      ranges.add(range);
      size += Long.bitCount(range.getBitmap()[0]);
      return;
    }
    // TODO: Implement binary search or something here in case the inserts are out of order.
    throw new UnsupportedOperationException("Inserts out of order");
  }

  @Override
  protected Collection<? extends Range> ranges() {
    return Collections.unmodifiableList(ranges);
  }

  private List<Range> fragment(Range r) {
    List<Range> rangeList = new ArrayList<Range>();
    long start = r.getStart();
    for (long l : r.getBitmap()) {
      rangeList.add(new BasicRange(start, new long[] { l }));
      start += Long.SIZE;
    }
    return rangeList;
  }

  private class OIDIterator implements Iterator<ObjectID> {
    private final Iterator<? extends Range> rangeIterator = ranges.iterator();

    private Range current;
    private long mask;

    private OIDIterator() {
      advance();
    }

    private void advance() {
       do {
        if (current == null) {
          if (rangeIterator.hasNext()) {
            current = rangeIterator.next();
            mask = 1;
          } else {
            mask = -1;
            return;
          }
        } else {
          mask <<= 1;
          if (mask == 0) {
            current = null;
          }
        }
      } while (current == null || (current.getBitmap()[0] & mask) == 0);
    }

    @Override
    public boolean hasNext() {
      return current != null && mask != 0;
    }

    @Override
    public ObjectID next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      ObjectID oid = new ObjectID(current.getStart() + Long.numberOfTrailingZeros(mask));
      advance();
      return oid;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("BasicObjectIDSet{");
    sb.append("ranges=").append(ranges);
    sb.append(", size=").append(size);
    sb.append('}');
    return sb.toString();
  }
}
