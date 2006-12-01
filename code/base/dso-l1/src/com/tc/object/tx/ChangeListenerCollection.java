/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.util.Assert;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * L1 side change listener collection. At the moment the class doesn't do too much except hide the fact that we are
 * using a CopyOnWriteArrayList as the actual implementation. In the future, we could hook into the add/remove methods
 * to intelligently register/de-register change event interest with L2. Currently, once you register for change events,
 * you'll ALWAYS get them forever
 */
public class ChangeListenerCollection implements Collection {
  private final Collection listeners  = new CopyOnWriteArrayList();
  private final String     callbackMethod;
  private final Map        methodRefs = new IdentityHashMap();
  private final Class[]    callbackSignature;

  public static class NoSuchCallbackException extends RuntimeException {
    public NoSuchCallbackException(String msg) {
      super(msg);
    }
  }

  public ChangeListenerCollection(String callbackMethod) {
    super();
    Assert.assertNotNull("callbackmethod", callbackMethod);
    this.callbackMethod = callbackMethod;
    this.callbackSignature = new Class[] { Iterator.class };
  }

  public synchronized boolean add(Object obj) {
    if (obj == null) { throw new NullPointerException("null not permitted in change listener collection"); }
    findCallback(obj);
    return listeners.add(obj);
  }

  private Method findCallback(Object o) {
    // NOTE: you might be tempted to store the callback references by object.getClass() (not by instance). If you do
    // that, you can't just drop the method on collection.remove(). You'd need to wait until all instance of that class
    // are removed from the collection. Either that or never remove method references once you've looked one up for each
    // class, but don't do that ;-). For now, store/remove a method reference per member of the collection

    Assert.assertNotNull(o);

    if (!methodRefs.containsKey(o)) {
      try {
        Method method = o.getClass().getDeclaredMethod(callbackMethod, callbackSignature);
        method.setAccessible(true);
        methodRefs.put(o, method);
      } catch (Exception e) {
        // make formatter sane
        throw new NoSuchCallbackException(e.getClass().getName() + " occured trying to locate callback method named "
                                          + callbackMethod + " on class " + o.getClass());
      }
    }

    return (Method) methodRefs.get(o);
  }

  public boolean addAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public synchronized void clear() {
    listeners.clear();
    methodRefs.clear();
  }

  public boolean contains(Object o) {
    return listeners.contains(o);
  }

  public boolean containsAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public boolean equals(Object obj) {
    return this == obj;
  }

  public int hashCode() {
    return listeners.hashCode();
  }

  public boolean isEmpty() {
    return listeners.isEmpty();
  }

  public Iterator iterator() {
    final Iterator internalIter = listeners.iterator();

    return new Iterator() {
      public void remove() {
        // if we're going to support this, we need a way to clean up the methodRefs collection upon remove
        throw new UnsupportedOperationException();
      }

      public boolean hasNext() {
        return internalIter.hasNext();
      }

      public Object next() {
        return internalIter.next();
      }
    };
  }

  public synchronized boolean remove(Object o) {
    if (null == o) { return false; }
    methodRefs.remove(o);
    return listeners.remove(o);
  }

  public boolean removeAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public boolean retainAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public int size() {
    return listeners.size();
  }

  public Object[] toArray() {
    return listeners.toArray();
  }

  public Object[] toArray(Object[] a) {
    return listeners.toArray(a);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer('[');

    final Iterator iter = listeners.iterator();
    boolean hasNext = iter.hasNext();
    while (hasNext) {
      Object obj = iter.next();
      buf.append(obj == this ? "(this collection)" : String.valueOf(obj));
      hasNext = iter.hasNext();
      if (hasNext) buf.append(", ");
    }

    return buf.append(']').toString();
  }

  synchronized Method getCallbackFor(Object obj) {
    Assert.assertNotNull(obj);
    return (Method) methodRefs.get(obj);
  }
}