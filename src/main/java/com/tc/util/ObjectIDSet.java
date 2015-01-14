package com.tc.util;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.ObjectID;

import java.io.IOException;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

/**
 * @author tim
 */
public abstract class ObjectIDSet extends AbstractSet<ObjectID> implements SortedSet<ObjectID>, TCSerializable {

  public static final ObjectIDSet EMPTY_OBJECT_ID_SET = unmodifiableObjectIDSet(new BasicObjectIDSet());
  
  protected static final Comparator<Range> RANGE_COMPARATOR = new Comparator<Range>() {
    @Override
    public int compare(final Range o1, final Range o2) {
      return (int) (o1.getStart() - o2.getStart());
    }
  };

  @Override
  public SortedSet<ObjectID> subSet(final ObjectID fromElement, final ObjectID toElement) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public SortedSet<ObjectID> headSet(final ObjectID toElement) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public SortedSet<ObjectID> tailSet(final ObjectID fromElement) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public ObjectID first() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public ObjectID last() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Comparator<? super ObjectID> comparator() {
    return new Comparator<ObjectID>() {
      @Override
      public int compare(final ObjectID o1, final ObjectID o2) {
        return o1.compareTo(o2);
      }
    };
  }

  /**
   * Implemented by a sub-class to take the given {@link Range} and add it into the current set.
   *
   * @param range object representing the range to be added
   */
  protected abstract void insertRange(Range range);

  /**
   * Implemented by sub-classes as a way to expose their internal ranges.
   *
   * @return {@link java.util.Collection} of the ranges in this set
   */
  protected abstract Collection<? extends Range> ranges();

  @Override
  public final void serializeTo(final TCByteBufferOutput serialOutput) {
    Collection<? extends Range> ranges = ranges();
    serialOutput.writeInt(ranges.size());
    for (Range range : ranges) {
      serialOutput.writeLong(range.getStart());
      serialOutput.writeInt(range.getBitmap().length);
      for (int i = 0; i < range.getBitmap().length; i++) {
        serialOutput.writeLong(range.getBitmap()[i]);
      }
    }
  }

  @Override
  public final Object deserializeFrom(final TCByteBufferInput serialInput) throws IOException {
    int size = serialInput.readInt();
    for (int i = 0; i < size; i++) {
      long start = serialInput.readLong();
      long[] bitmap = new long[serialInput.readInt()];
      for (int j = 0; j < bitmap.length; j++) {
        bitmap[j] = serialInput.readLong();
      }
      insertRange(new BasicRange(start, bitmap));
    }
    return this;
  }

  protected interface Range {
    long getStart();
    long[] getBitmap();
  }

  protected static class BasicRange implements Range {
    private final long start;
    private final long[] bitmap;

    BasicRange(final long start, final long[] bitmap) {
      this.start = start;
      this.bitmap = bitmap;
    }

    @Override
    public long getStart() {
      return start;
    }

    @Override
    public long[] getBitmap() {
      return bitmap;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("BasicRange{");
      sb.append("start=").append(start);
      sb.append(", bitmap=").append(Arrays.toString(bitmap));
      sb.append('}');
      return sb.toString();
    }
  }

  public static ObjectIDSet unmodifiableObjectIDSet(final ObjectIDSet s) {
    return new UnmodifiableObjectIDSet(s);
  }

  private static class UnmodifiableObjectIDSet extends ObjectIDSet {
    private final ObjectIDSet delegate;

    private UnmodifiableObjectIDSet(final ObjectIDSet delegate) {
      this.delegate = delegate;
    }

    @Override
    protected void insertRange(final Range range) {
      throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    protected Collection<? extends Range> ranges() {
      return delegate.ranges();
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public Iterator<ObjectID> iterator() {
      return new Iterator<ObjectID>() {
        Iterator<ObjectID> i = delegate.iterator();

        @Override
        public boolean hasNext() {
          return this.i.hasNext();
        }

        @Override
        public ObjectID next() {
          return this.i.next();
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public boolean add(final ObjectID id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(final Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(final Collection<? extends ObjectID> coll) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(final Collection<?> coll) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(final Collection<?> coll) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

  }
}
