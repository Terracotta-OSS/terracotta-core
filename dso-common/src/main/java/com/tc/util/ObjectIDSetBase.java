/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

abstract class ObjectIDSetBase extends AbstractSet<ObjectID> implements SortedSet<ObjectID>, PrettyPrintable,
    TCSerializable {
  protected transient volatile int modCount;
  protected final AATreeSet        ranges;
  protected int                    size = 0;

  public ObjectIDSetBase() {
    this.ranges = new AATreeSet();
  }

  public abstract Object deserializeFrom(TCByteBufferInput in) throws IOException;

  public abstract void serializeTo(TCByteBufferOutput out);

  @Override
  public abstract Iterator iterator();

  public abstract boolean contains(ObjectID id);

  public abstract boolean remove(ObjectID id);

  @Override
  public boolean remove(Object o) {
    if (o instanceof ObjectID) {
      return remove((ObjectID) o);
    } else {
      return false;
    }
  }

  @Override
  public abstract boolean add(ObjectID id);

  private String getObjectIDSetType() {
    return this.getClass().getSimpleName();
  }

  @Override
  public int size() {
    return this.size;
  }

  @Override
  public String toString() {
    if (size() <= 200) { return toVerboseString(); }

    StringBuffer sb = new StringBuffer(getObjectIDSetType() + " " + getCompressionDetails() + "[");
    for (Iterator i = this.ranges.iterator(); i.hasNext();) {
      sb.append(' ').append(i.next());
    }
    return sb.append(']').toString();
  }

  public String toVerboseString() {

    StringBuffer sb = new StringBuffer(getObjectIDSetType() + " [ ");
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
    StringBuffer sb = new StringBuffer(getObjectIDSetType() + " " + getCompressionDetails() + "[");
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

  @Override
  public boolean contains(Object o) {
    if (o instanceof ObjectID) {
      return contains((ObjectID) o);
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

  public abstract ObjectID first();

  public abstract ObjectID last();

  public SortedSet headSet(ObjectID arg0) {
    throw new UnsupportedOperationException();
  }

  public SortedSet subSet(ObjectID arg0, ObjectID arg1) {
    throw new UnsupportedOperationException();
  }

  public SortedSet tailSet(ObjectID arg0) {
    throw new UnsupportedOperationException();
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print(toShortString());
    return out;
  }

}
