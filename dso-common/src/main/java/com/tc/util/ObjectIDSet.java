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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
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
public class ObjectIDSet extends AbstractSet<ObjectID> implements SortedSet<ObjectID>, PrettyPrintable, TCSerializable {

  public static enum ObjectIDSetType {
    RANGE_BASED_SET, BITSET_BASED_SET
  }

  /**
   * modCount - number of times this HashMap has been structurally modified Structural modifications are those that
   * change the number of mappings in the HashMap or otherwise modify its internal structure (e.g., rehash). This field
   * is used to make iterators on Collection-views of the HashMap fail-fast. (See ConcurrentModificationException).
   */

  private ObjectIDSetType type;
  private ObjectIDSetBase oidSet;

  public ObjectIDSet() {
    this.type = ObjectIDSetType.BITSET_BASED_SET;
    this.oidSet = new BitSetObjectIDSet();
  }

  public ObjectIDSet(final ObjectIDSetType oidSetType) {
    if (oidSetType == ObjectIDSetType.RANGE_BASED_SET) {
      this.type = ObjectIDSetType.RANGE_BASED_SET;
      this.oidSet = new RangeObjectIDSet();
    } else {
      this.type = ObjectIDSetType.BITSET_BASED_SET;
      this.oidSet = new BitSetObjectIDSet();
    }
  }

  public ObjectIDSet(final Collection c) {
    if (c instanceof ObjectIDSet) {
      final ObjectIDSet o = (ObjectIDSet) c;
      if (o.type == ObjectIDSetType.BITSET_BASED_SET) {
        this.type = ObjectIDSetType.BITSET_BASED_SET;
        this.oidSet = new BitSetObjectIDSet(o.oidSet);
      } else if (o.type == ObjectIDSetType.RANGE_BASED_SET) {
        this.type = ObjectIDSetType.RANGE_BASED_SET;
        this.oidSet = new RangeObjectIDSet(o.oidSet);
      } else {
        throw new AssertionError("wrong ObjectIDSet type: " + o.type);
      }
    } else {
      this.type = ObjectIDSetType.BITSET_BASED_SET;
      this.oidSet = new BitSetObjectIDSet(c);
    }
  }

  public ObjectIDSet(final Collection c, final ObjectIDSetType oidSetType) {
    if (oidSetType == ObjectIDSetType.RANGE_BASED_SET) {
      this.type = ObjectIDSetType.RANGE_BASED_SET;
      this.oidSet = new RangeObjectIDSet(c);
    } else {
      this.type = ObjectIDSetType.BITSET_BASED_SET;
      this.oidSet = new BitSetObjectIDSet(c);
    }
  }

  /* Used by Unmodifiable ObjectIDSet */
  private ObjectIDSet(ObjectIDSetType type, ObjectIDSetBase oidSet) {
    this.type = type;
    this.oidSet = oidSet;
  }

  public Object deserializeFrom(final TCByteBufferInput in) throws IOException {
    int oidSetType = in.readInt();
    if (oidSetType == ObjectIDSetType.RANGE_BASED_SET.ordinal()) {
      this.type = ObjectIDSetType.RANGE_BASED_SET;
      this.oidSet = new RangeObjectIDSet();
    } else if (oidSetType == ObjectIDSetType.BITSET_BASED_SET.ordinal()) {
      this.type = ObjectIDSetType.BITSET_BASED_SET;
      this.oidSet = new BitSetObjectIDSet();
    } else {
      throw new AssertionError("wrong type: " + oidSetType);
    }
    this.oidSet.deserializeFrom(in);
    return this;
  }

  public void serializeTo(final TCByteBufferOutput out) {
    out.writeInt(this.type.ordinal());
    this.oidSet.serializeTo(out);
  }

  @Override
  public Iterator iterator() {
    return this.oidSet.iterator();
  }

  @Override
  public int size() {
    return this.oidSet.size();
  }

  public boolean contains(final ObjectID id) {
    return this.oidSet.contains(id);
  }

  @Override
  public boolean contains(final Object id) {
    return this.oidSet.contains(id);
  }

  public boolean remove(final ObjectID id) {
    return this.oidSet.remove(id);
  }

  @Override
  public boolean remove(final Object o) {
    return this.oidSet.remove(o);
  }

  @Override
  public boolean removeAll(final Collection c) {
    return this.oidSet.removeAll(c);
  }

  @Override
  public void clear() {
    this.oidSet.clear();
  }

  @Override
  public boolean add(final ObjectID id) {
    return this.oidSet.add(id);
  }

  /**
   * Optimized version of addAll to make it faster for ObjectIDSet
   */
  @Override
  public boolean addAll(final Collection c) {
    if (c instanceof ObjectIDSet) {
      final ObjectIDSet o = (ObjectIDSet) c;
      if (o.type == this.type) {
        if (this.type == ObjectIDSetType.BITSET_BASED_SET) {
          return ((BitSetObjectIDSet) this.oidSet).addAll((BitSetObjectIDSet) o.oidSet);
        } else if (this.type == ObjectIDSetType.RANGE_BASED_SET) {
          return ((RangeObjectIDSet) this.oidSet).addAll((RangeObjectIDSet) o.oidSet);
        } else {
          throw new AssertionError("wrong ObjectIDSet type: " + o.type);
        }
      }
    }
    return super.addAll(c);
  }

  @Override
  public String toString() {
    return this.oidSet.toString();
  }

  public String toVerboseString() {
    return this.oidSet.toVerboseString();
  }

  public String toShortString() {
    return this.oidSet.toShortString();
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(toVerboseString());
    return out;
  }

  // =======================SortedSet Interface Methods==================================

  public ObjectID first() {
    return this.oidSet.first();
  }

  public ObjectID last() {
    return this.oidSet.last();
  }

  public Comparator comparator() {
    return null;
  }

  public SortedSet headSet(final ObjectID arg0) {
    throw new UnsupportedOperationException();
  }

  public SortedSet subSet(final ObjectID arg0, final ObjectID arg1) {
    throw new UnsupportedOperationException();
  }

  public SortedSet tailSet(final ObjectID arg0) {
    throw new UnsupportedOperationException();
  }

  // =======================Unmodifiable ObjectIDSet Methods==================================

  public static ObjectIDSet unmodifiableObjectIDSet(final ObjectIDSet s) {
    return new UnmodifiableObjectIDSet(s);
  }

  static class UnmodifiableObjectIDSet extends ObjectIDSet {

    UnmodifiableObjectIDSet(final ObjectIDSet s) {
      super(s.type, s.oidSet);
    }

    @Override
    public Iterator iterator() {
      return new Iterator() {
        Iterator i = UnmodifiableObjectIDSet.super.iterator();

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
    public boolean remove(final ObjectID id) {
      throw new UnsupportedOperationException();
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
    public boolean addAll(final Collection coll) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(final Collection coll) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(final Collection coll) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

  }

}
