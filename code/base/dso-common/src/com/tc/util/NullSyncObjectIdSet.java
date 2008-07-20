/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.text.PrettyPrinter;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public class NullSyncObjectIdSet extends AbstractSet implements SyncObjectIdSet {

  public void startPopulating() {
    throw new UnsupportedOperationException();
  }

  public void stopPopulating(ObjectIDSet fullSet) {
    throw new UnsupportedOperationException();
  }

  public boolean add(Object obj) {
    return true;
  }

  public boolean contains(Object o) {
    return true;
  }

  public boolean removeAll(Collection ids) {
    return true;
  }

  public boolean remove(Object o) {
    return true;
  }

  public Iterator iterator() {
    throw new UnsupportedOperationException();
  }

  public int size() {
    return 0;
  }

  public ObjectIDSet snapshot() {
    throw new UnsupportedOperationException();
  }


  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out;
  }
}
