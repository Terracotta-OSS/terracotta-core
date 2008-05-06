/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.collections;

import com.tc.util.Assert;
import com.tc.util.EqualityComparator;
import com.tc.util.Stringifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * An object that knows how to compare collections of objects for equality. Unlike using the simple
 * {@link Collection.equals(Object)}method, this class (or subclasses) can compare collections under various
 * constraints (e.g., must be the same, but without regard to order; must have the same objects, but number of
 * repetitions of those objects is irrelevant; etc.). Also, this class reports mismatches between collections in a
 * uniform, object-oriented way, such that they can be easily printed out, compared themselves, and so on.
 */
public abstract class CollectionComparer {

  private final EqualityComparator comparator;
  private final Stringifier  describer;

  public CollectionComparer(EqualityComparator comparator, Stringifier describer) {
    Assert.assertNotNull(comparator);
    Assert.assertNotNull(describer);

    this.comparator = comparator;
    this.describer = describer;
  }

  public CollectionMismatch[] getMismatches(Object collectionOneObject, Object collectionTwoObject) {
    Assert.assertNotNull(collectionOneObject);
    Assert.assertNotNull(collectionTwoObject);

    Object[] collectionOne = getCollection(collectionOneObject);
    Object[] collectionTwo = getCollection(collectionTwoObject);

    return doComparison(collectionOne, collectionTwo);
  }

  protected abstract CollectionMismatch[] doComparison(Object[] collectionOne, Object[] collectionTwo);

  protected final Stringifier describer() {
    return this.describer;
  }

  protected final boolean isEqual(Object firstObject, boolean firstIsOne, Object secondObject,
                                  boolean secondIsOne, int oneIndex, int twoIndex) {
    boolean isEqual = this.comparator.isEquals(firstObject, secondObject);
    boolean flipEqual = this.comparator.isEquals(secondObject, firstObject);

    if (isEqual != flipEqual) {
      // formatting
      throw new IllegalStateException("Your comparator is broken; it claimed that collection "
                                      + (firstIsOne ? "one" : "two") + ", index " + oneIndex + " ("
                                      + this.describer.toString(firstObject) + ") was " + (isEqual ? "" : "not ")
                                      + "equal to collection " + (secondIsOne ? "one" : "two") + ", index " + twoIndex
                                      + " (" + (this.describer.toString(secondObject)) + "), but the reverse "
                                      + "comparison gave the opposite result.");
    }

    return isEqual;
  }

  private Object[] getCollection(Object o) {
    Assert.assertNotNull(o);

    if (o instanceof Object[]) return (Object[]) o;
    else if (o instanceof byte[]) {
      return getCollection((byte[]) o);
    } else if (o instanceof char[]) {
      return getCollection((char[]) o);
    } else if (o instanceof short[]) {
      return getCollection((short[]) o);
    } else if (o instanceof int[]) {
      return getCollection((int[]) o);
    } else if (o instanceof long[]) {
      return getCollection((long[]) o);
    } else if (o instanceof boolean[]) {
      return getCollection((boolean[]) o);
    } else if (o instanceof float[]) {
      return getCollection((float[]) o);
    } else if (o instanceof double[]) {
      return getCollection((double[]) o);
    } else if (o instanceof Iterator) {
      return getCollection((Iterator) o);
    } else if (o instanceof Collection) {
      return getCollection((Collection) o);
    } else {
      throw new IllegalArgumentException("This object, " + o + ", is of class '" + o.getClass().getName() + "'; "
                                         + "that is not a recognized type of collection or other aggregate.");
    }
  }

  private Object[] getCollection(byte[] b) {
    Object[] o = new Object[b.length];
    for (int i = 0; i < o.length; ++i) {
      o[i] = new Byte(b[i]);
    }
    return o;
  }

  private Object[] getCollection(char[] c) {
    Object[] o = new Object[c.length];
    for (int i = 0; i < o.length; ++i) {
      o[i] = new Character(c[i]);
    }
    return o;
  }

  private Object[] getCollection(short[] s) {
    Object[] o = new Object[s.length];
    for (int i = 0; i < o.length; ++i) {
      o[i] = new Short(s[i]);
    }
    return o;
  }

  private Object[] getCollection(int[] iarr) {
    Object[] o = new Object[iarr.length];
    for (int i = 0; i < o.length; ++i) {
      o[i] = new Integer(iarr[i]);
    }
    return o;
  }

  private Object[] getCollection(long[] l) {
    Object[] o = new Object[l.length];
    for (int i = 0; i < o.length; ++i) {
      o[i] = new Long(l[i]);
    }
    return o;
  }

  private Object[] getCollection(boolean[] b) {
    Object[] o = new Object[b.length];
    for (int i = 0; i < o.length; ++i) {
      o[i] = b[i] ? Boolean.TRUE : Boolean.FALSE;
    }
    return o;
  }

  private Object[] getCollection(float[] f) {
    Object[] o = new Object[f.length];
    for (int i = 0; i < o.length; ++i) {
      o[i] = new Float(f[i]);
    }
    return o;
  }

  private Object[] getCollection(double[] d) {
    Object[] o = new Object[d.length];
    for (int i = 0; i < o.length; ++i) {
      o[i] = new Double(d[i]);
    }
    return o;
  }

  private Object[] getCollection(Iterator i) {
    List out = new ArrayList();
    while (i.hasNext())
      out.add(i.next());
    return out.toArray(new Object[out.size()]);
  }

  private Object[] getCollection(Collection c) {
    return c.toArray(new Object[c.size()]);
  }

}