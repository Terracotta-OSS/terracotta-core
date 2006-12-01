/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;

import junit.framework.TestCase;

public class ChangeListenerCollectionTest extends TestCase {

  ChangeListenerCollection collection = new ChangeListenerCollection("callback");

  private static class Bad {
    // NOTE: this class does NOT have the proper callback method (ie. callback(Itertor) )

    void callback() {
      //
    }

    void callback(Object arg) {
      //
    }

    void callback(Iterator iter, Object arg) {
      //
    }

  }

  // This method private on purpose. The callback method is allowed to have any access flags
  private void callback(Iterator changes) {
    fail("shouldn't be called during test");
  }

  void DONT_CALL_ME() {
    // this method here simply to silence compiler warning about callback() never being called
    callback(null);
  }

  public void testAddRemove() throws Exception {
    try {
      collection.add(null);
      fail("allowed to add null");
    } catch (NullPointerException npe) {
      // expected
    }

    // test add with instance that has the callback method
    assertFalse(collection.contains(this));
    assertEquals(0, collection.size());
    assertNull(collection.getCallbackFor(this));
    collection.add(this);
    assertTrue(collection.contains(this));
    assertEquals(1, collection.size());
    Method callback = collection.getCallbackFor(this);
    assertNotNull(callback);
    assertEquals(getClass().getDeclaredMethod("callback", new Class[] { Iterator.class }), callback);

    // test remove
    collection.remove(this);
    assertEquals(0, collection.size());
    assertNull(collection.getCallbackFor(this));

    // test adding instance w/o the callback method
    assertEquals(0, collection.size());
    try {
      collection.add(new Bad());
      fail("allowed to add an object w/o the callback");
    } catch (ChangeListenerCollection.NoSuchCallbackException nsce) {
      // expected
    }
    assertEquals(0, collection.size());
  }

  public void testAddAll() {
    try {
      collection.addAll(new LinkedList());
      fail("someone implemented this method but didn't update/implement the test");
    } catch (UnsupportedOperationException uoe) {
      // expected
    }
  }

  public void testClear() {
    collection.add(this);
    ChangeListenerCollectionTest that = new ChangeListenerCollectionTest();
    collection.add(that);
    assertEquals(2, collection.size());
    assertNotNull(collection.getCallbackFor(this));
    assertNotNull(collection.getCallbackFor(that));

    collection.clear();

    assertEquals(0, collection.size());
    assertNull(collection.getCallbackFor(this));
    assertNull(collection.getCallbackFor(that));
  }

  public void testContains() {
    assertFalse(collection.contains(this));
    collection.add(this);
    assertTrue(collection.contains(this));
    collection.remove(this);
    assertFalse(collection.contains(this));
  }

  public void testContainsAll() {
    try {
      collection.containsAll(new LinkedList());
      fail("someone implemented this method but didn't update/implement the test");
    } catch (UnsupportedOperationException uoe) {
      // expected
    }
  }

  public void testIsEmpty() {
    assertTrue(collection.isEmpty());
    collection.add(this);
    assertFalse(collection.isEmpty());
  }

  public void testIterator() {
    Iterator iter = collection.iterator();
    assertFalse(iter.hasNext());

    collection.add(this);
    iter = collection.iterator();
    assertTrue(iter.hasNext());
    assertSame(this, iter.next());
    assertFalse(iter.hasNext());


    iter = collection.iterator();
    iter.next();
    try {
      iter.remove();
      fail("iterator supports remove()");
    }
    catch (UnsupportedOperationException uoe) {
      // expected
    }
  }

  public void testRemoveAll() {
    try {
      collection.removeAll(new LinkedList());
      fail("someone implemented this method but didn't update/implement the test");
    } catch (UnsupportedOperationException uoe) {
      // expected
    }
  }

  public void testRetainAll() {
    try {
      collection.retainAll(new LinkedList());
      fail("someone implemented this method but didn't update/implement the test");
    } catch (UnsupportedOperationException uoe) {
      // expected
    }
  }

  public void testSize() {
    assertEquals(0, collection.size());
    collection.add(this);
    assertEquals(1, collection.size());
    collection.remove(this);
    assertEquals(0, collection.size());
  }

  public void testToArray() {
    assertEquals(0, collection.toArray().length);
    collection.add(this);
    Object objs[] = collection.toArray();
    assertEquals(1, objs.length);
    assertSame(this, objs[0]);
  }

  public void testToString() {
    collection.toString();
    collection.add(this);
    collection.toString();
  }

}