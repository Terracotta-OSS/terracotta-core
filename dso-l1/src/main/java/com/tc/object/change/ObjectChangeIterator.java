/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.change;

import java.util.Collection;
import java.util.Iterator;

/**
 * The iterator passed to customer change callback methods. The most important feature of this class is that is does not
 * allow remove() to be called
 */
public class ObjectChangeIterator implements Iterator {
  private final Iterator   iter;

  public ObjectChangeIterator(Collection objects) {
    this.iter = objects.iterator();
  }

  public void remove() {
    throw new UnsupportedOperationException("remove() not suppored");
  }

  public boolean hasNext() {
    return this.iter.hasNext();
  }

  public Object next() {
    return this.iter.next();
  }
}