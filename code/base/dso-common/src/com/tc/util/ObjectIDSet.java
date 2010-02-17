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
public class ObjectIDSet extends AbstractSet<ObjectID> implements SortedSet<ObjectID>, PrettyPrintable,
    TCSerializable {

  public static enum ObjectIDSetType {
    RANGE_BASED_SET, BITSET_BASED_SET
  }

  /**
   * modCount - number of times this HashMap has been structurally modified Structural modifications are those that
   * change the number of mappings in the HashMap or otherwise modify its internal structure (e.g., rehash). This field
   * is used to make iterators on Collection-views of the HashMap fail-fast. (See ConcurrentModificationException).
   */

  private int             type;
  private ObjectIDSetBase oidSet;

  public ObjectIDSet() {
    this.type = ObjectIDSetType.BITSET_BASED_SET.ordinal();
    this.oidSet = new BitSetObjectIDSet();
  }

  public ObjectIDSet(ObjectIDSetType oidSetType) {
    if (oidSetType == ObjectIDSetType.RANGE_BASED_SET) {
      this.type = ObjectIDSetType.RANGE_BASED_SET.ordinal();
      this.oidSet = new RangeObjectIDSet();
    } else {
      this.type = ObjectIDSetType.BITSET_BASED_SET.ordinal();
      this.oidSet = new BitSetObjectIDSet();
    }
  }

  public ObjectIDSet(Collection c) {
    if (c instanceof ObjectIDSet) {
      ObjectIDSet o = (ObjectIDSet) c;
      if (o.type == ObjectIDSetType.BITSET_BASED_SET.ordinal()) {
        this.type = ObjectIDSetType.BITSET_BASED_SET.ordinal();
        this.oidSet = new BitSetObjectIDSet(o.oidSet);
      } else if (o.type == ObjectIDSetType.RANGE_BASED_SET.ordinal()) {
        this.type = ObjectIDSetType.RANGE_BASED_SET.ordinal();
        this.oidSet = new RangeObjectIDSet(o.oidSet);
      }else{
        throw new AssertionError("wrong ObjectIDSet type: " + o.type);
      }
    } else {
      this.type = ObjectIDSetType.BITSET_BASED_SET.ordinal();
      this.oidSet = new BitSetObjectIDSet(c);
    }
  }

  public ObjectIDSet(Collection c, ObjectIDSetType oidSetType) {
    if (oidSetType == ObjectIDSetType.RANGE_BASED_SET) {
      this.type = ObjectIDSetType.RANGE_BASED_SET.ordinal();
      this.oidSet = new RangeObjectIDSet(c);
    } else {
      this.type = ObjectIDSetType.BITSET_BASED_SET.ordinal();
      this.oidSet = new BitSetObjectIDSet(c);
    }
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    this.type = in.readInt();
    if (this.type == ObjectIDSetType.RANGE_BASED_SET.ordinal()) {
      this.oidSet = new RangeObjectIDSet();
      this.oidSet.deserializeFrom(in);
    } else if (this.type == ObjectIDSetType.BITSET_BASED_SET.ordinal()) {
      this.oidSet = new BitSetObjectIDSet();
      this.oidSet.deserializeFrom(in);
    } else {
      throw new AssertionError("wrong type: " + this.type);
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput out) {
    out.writeInt(this.type);
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

  public boolean contains(ObjectID id) {
    return this.oidSet.contains(id);
  }

   @Override
  public boolean contains(Object id) {
    return this.oidSet.contains(id);
  }

  public boolean remove(ObjectID id) {
    return this.oidSet.remove(id);
  }
  
  @Override
  public boolean remove(Object o) {
    return this.oidSet.remove(o);
  }

  @Override
  public boolean removeAll(Collection c) {
    return this.oidSet.removeAll(c);
  }

  @Override
  public void clear() {
    this.oidSet.clear();
  }

  @Override
  public boolean add(ObjectID id) {
    return this.oidSet.add(id);
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

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print(toShortString());
    return out;
  }

  // =======================SortedSet Interface Methods==================================

  public ObjectID first() {
    return this.oidSet.first();
  }

  public ObjectID last() {
    return this.oidSet.last();
  }

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

  // =======================SortedSet Interface Methods==================================

  public Comparator comparator() {
    return null;
  }

  public SortedSet headSet(ObjectID arg0) {
    throw new UnsupportedOperationException();
  }

  public SortedSet subSet(ObjectID arg0, ObjectID arg1) {
    throw new UnsupportedOperationException();
  }

  public SortedSet tailSet(ObjectID arg0) {
    throw new UnsupportedOperationException();
  }

}
