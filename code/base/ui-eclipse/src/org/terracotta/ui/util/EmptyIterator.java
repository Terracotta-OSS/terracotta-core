/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class EmptyIterator implements Iterator {
  public boolean hasNext() {
    return false;
  }

  public Object next() {
    throw new NoSuchElementException();
  }

  public void remove() {
    throw new IllegalStateException();
  }
}
