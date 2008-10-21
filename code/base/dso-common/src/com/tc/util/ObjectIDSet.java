/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class was build in an attempt to store a large set of ObjectIDs compressed in memory while giving the same
 * characteristic of HashSet in terms of performance.
 * <p>
 * This is version 2 of the class, the older one version 1 has several shortcomings. Mainly the performance of adds and
 * removes when the ObjectIDs are non-contiguous.
 * <p>
 * This one uses a balanced tree internally to store ranges instead of an ArrayList
 */
public class ObjectIDSet extends AbstractSet implements SortedSet, PrettyPrintable, Serializable {

  static final int MIN_JUMBO_SIZE = 20;
  private boolean jumbo = false;
  private SortedSet internalSet;
  
  public ObjectIDSet() {
    internalSet = new TreeSet();
  }

  public ObjectIDSet(Collection c) {
    if(c.size() > MIN_JUMBO_SIZE) {
      internalSet = new JumboObjectIDSet(c);
      jumbo = true;
    } else {
      internalSet = new TreeSet(c);
    }
  }

  public Iterator iterator() {
    return internalSet.iterator();
  }

  public int size() {
    return internalSet.size();
  }

  public String toString() {
    return internalSet.toString();
  }

  public String toShortString() {
    if(jumbo) {
      return ((JumboObjectIDSet)internalSet).toShortString();
    } else {
      return internalSet.toString();
    }
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(toShortString());
    return out;
  }

 
  public boolean contains(Object o) {
    return internalSet.contains(o);
  }

  public boolean add(Object arg0) {
    if(!jumbo && internalSet.size() >= MIN_JUMBO_SIZE) {
      SortedSet tinySet = this.internalSet;
      internalSet = new JumboObjectIDSet(tinySet);
      jumbo = true;
    }
    return internalSet.add(arg0);
  }

  public boolean remove(Object o) {
    // Could shrink from jumbo to tiny set here, but not worth it
    return internalSet.remove(o);
  }

  public void clear() {
    this.internalSet.clear();
  }

  // =======================SortedSet Interface Methods==================================

  public Comparator comparator() {
    return null;
  }

  public Object first() {
    return this.internalSet.first();
  }

  public Object last() {
    return this.internalSet.last();
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
}
