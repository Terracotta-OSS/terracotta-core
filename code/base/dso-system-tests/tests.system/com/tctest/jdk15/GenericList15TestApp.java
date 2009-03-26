/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.GenericTransparentApp;

import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;

/**
 * This contains the same test cases of GenericListTestApp, plus the jdk1.5 specific test cases.
 */
public class GenericList15TestApp extends GenericTransparentApp {

  public GenericList15TestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, List.class);
  }

  protected Object getTestObject(String testName) {
    List lists = (List) sharedMap.get("lists");
    return lists.iterator();
  }

  protected void setupTestObject(String testName) {
    List lists = new ArrayList();
    lists.add(new LinkedList());
    lists.add(new ArrayList());
    lists.add(new Vector());
    lists.add(new Stack());

    sharedMap.put("lists", lists);
    sharedMap.put("arrayforLinkedList", new Object[2]);
    sharedMap.put("arrayforArrayList", new Object[2]);
    sharedMap.put("arrayforVector", new Object[2]);
    sharedMap.put("arrayforStack", new Object[2]);
  }

  void testBasicAdd(List list, boolean validate) {
    if (validate) {
      assertSingleElement(list, "rollin in my 6-4");
    } else {
      synchronized (list) {
        boolean added = list.add("rollin in my 6-4");
        Assert.assertTrue(added);
      }
    }
  }

  void testVectorSetSizeGrow(List list, boolean validate) {
    if (!(list instanceof Vector)) { return; }

    int size = 5;
    Vector v = (Vector) list;

    if (validate) {
      Assert.assertEquals("start", v.get(0));
      for (int i = 1; i < size; i++) {
        Object val = v.get(i);
        Assert.assertNull("element " + i + " is " + val, val);
      }
      Assert.assertEquals("end", v.get(size));
    } else {
      synchronized (v) {
        v.add("start");
        v.setSize(size);
        v.add("end");
      }
    }
  }

  void testVectorSetSizeShrink(List list, boolean validate) {
    if (!(list instanceof Vector)) { return; }

    Vector v = (Vector) list;

    if (validate) {
      Assert.assertEquals("start", v.get(0));
      Assert.assertEquals("end", v.get(1));
    } else {
      synchronized (v) {
        v.add("start");
        v.add("ho hum");
        v.add("ho hum2");
        v.add("ho hum3");

        v.setSize(1);
        v.add("end");
      }
    }
  }

  void testVectorAddElement(List list, boolean validate) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), vector);
    } else {
      synchronized (vector) {
        vector.addElement("first element");
        vector.addElement("second element");
      }
    }
  }

  void testVectorRetainAll(List list, boolean validate) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "second element" }), vector);
    } else {
      synchronized (vector) {
        vector.addElement("first element");
        vector.addElement("second element");
        vector.addElement("third element");

        List retainList = new ArrayList(1);
        retainList.add("second element");

        vector.retainAll(retainList);
      }
    }
  }

  void testVectorRemoveAll(List list, boolean validate) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "third element" }), vector);
    } else {
      synchronized (vector) {
        vector.addElement("first element");
        vector.addElement("second element");
        vector.addElement("third element");

        List removeList = new ArrayList(1);
        removeList.add("second element");
        vector.removeAll(removeList);
      }
    }
  }

  void testVectorRemoveAllElements(List list, boolean validate) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertEmptyList(vector);
    } else {
      synchronized (vector) {
        vector.addElement("first element");
        vector.addElement("second element");
        vector.addElement("third element");

        vector.removeAllElements();
      }
    }
  }

  void testVectorSetElementAt(List list, boolean validate) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element", "third element" }), vector);
    } else {
      synchronized (vector) {
        vector.addElement("first element");
        vector.addElement(null);
        vector.addElement("third element");

        vector.setElementAt("second element", 1);
      }
    }
  }

  void testVectorInsertElementAt(List list, boolean validate) {
    if (!(list instanceof Vector)) { return; }

    Vector vector = (Vector) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element", "third element" }), vector);
    } else {
      synchronized (vector) {
        vector.addElement("first element");
        vector.addElement("third element");

        vector.insertElementAt("second element", 1);
      }
    }
  }

  void testLinkedListRemoveFirst(List list, boolean validate) {
    if (!(list instanceof LinkedList)) { return; }

    LinkedList linkedList = (LinkedList) list;

    if (validate) {
      assertSingleElement(linkedList, "teck");
    } else {
      synchronized (linkedList) {
        linkedList.add("timmy");
        linkedList.add("teck");
      }

      synchronized (linkedList) {
        linkedList.removeFirst();
      }
    }
  }

  void testLinkedListRemoveLast(List list, boolean validate) {
    if (!(list instanceof LinkedList)) { return; }

    LinkedList linkedList = (LinkedList) list;

    if (validate) {
      assertSingleElement(linkedList, "timmy");
    } else {
      synchronized (linkedList) {
        linkedList.add("timmy");
        linkedList.add("teck");
      }

      synchronized (linkedList) {
        linkedList.removeLast();
      }
    }
  }

  void testLinkedListAddFirst(List list, boolean validate) {
    if (!(list instanceof LinkedList)) { return; }

    LinkedList linkedList = (LinkedList) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (linkedList) {
        linkedList.add("second element");
      }

      synchronized (linkedList) {
        linkedList.addFirst("first element");
      }
    }
  }

  void testLinkedListAddLast(List list, boolean validate) {
    if (!(list instanceof LinkedList)) { return; }

    LinkedList linkedList = (LinkedList) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (linkedList) {
        linkedList.add("first element");
      }

      synchronized (linkedList) {
        linkedList.addLast("second element");
      }
    }
  }

  void testLinkedListPoll(List list, boolean validate) {
    if (!(list instanceof LinkedList)) { return; }

    LinkedList linkedList = (LinkedList) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "second element" }), list);
    } else {
      synchronized (linkedList) {
        linkedList.add("first element");
        linkedList.add("second element");
      }

      synchronized (linkedList) {
        Object o = linkedList.poll();
        Assert.assertEquals("first element", o);
      }
    }
  }

  void testLinkedListOffer(List list, boolean validate) {
    if (!(list instanceof LinkedList)) { return; }

    LinkedList linkedList = (LinkedList) list;

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (linkedList) {
        linkedList.add("first element");
      }

      synchronized (linkedList) {
        linkedList.offer("second element");
      }
    }
  }

  void testBasicAddNull(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { null, null, "my cat hates you", null }), list);
    } else {
      synchronized (list) {
        boolean added;
        added = list.add(null);
        Assert.assertTrue(added);
        added = list.add(null);
        Assert.assertTrue(added);
        added = list.add("my cat hates you");
        Assert.assertTrue(added);
        added = list.add(null);
        Assert.assertTrue(added);
      }
    }
  }

  void testBasicAddAt(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "1", "2", "3", "4" }), list);
    } else {
      synchronized (list) {
        list.add(0, "2");
      }
      synchronized (list) {
        list.add(0, "1");
      }
      synchronized (list) {
        list.add(2, "4");
      }
      synchronized (list) {
        list.add(2, "3");
      }
    }
  }

  void testAdd(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "element" }), list);
    } else {
      synchronized (list) {
        list.add("element");
      }
    }
  }

  void testAddAll(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "patty", "calahan", "was", "here" }), list);
    } else {
      List toAdd = new ArrayList();
      toAdd.add("patty");
      toAdd.add("calahan");
      toAdd.add("was");
      toAdd.add("here");

      synchronized (list) {
        list.addAll(toAdd);
      }
    }
  }

  void testAddAllAt(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "uno", "dos", "tres", "catorce?" }), list);
    } else {
      synchronized (list) {
        list.add("uno");
      }

      List toAdd = new ArrayList();
      toAdd.add("dos");
      toAdd.add("tres");
      toAdd.add("catorce?");

      synchronized (list) {
        list.addAll(1, toAdd);
      }
    }
  }

  void testClear(List list, boolean validate) {
    if (validate) {
      assertEmptyList(list);
    } else {
      synchronized (list) {
        list.add("clear me baby");
        list.clear();
      }

      synchronized (list) {
        list.add("clear me baby one more time");
      }

      synchronized (list) {
        list.clear();
      }
    }
  }

  void testSetElementAt(List list, boolean validate) {
    if (validate) {
      assertSingleElement(list, "new");
    } else {
      synchronized (list) {
        list.add("orig");
      }

      synchronized (list) {
        list.set(0, "new");
      }
    }
  }

  void testRemoveAt(List list, boolean validate) {
    if (validate) {
      String item0 = (String) list.get(0);
      String item1 = (String) list.get(1);
      Assert.assertEquals("value", item0);
      Assert.assertEquals("different value", item1);
    } else {
      synchronized (list) {
        list.add("value");
        list.add("different value");
        list.add("value");
      }

      synchronized (list) {
        Object prev = list.remove(2);
        Assert.assertEquals("value", prev);
      }
    }
  }

  void testRemoveNull1(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", null, "third element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add(null);
        list.add(null);
        list.add("third element");
      }
      synchronized (list) {
        list.remove(null);
      }
    }
  }

  void testRemoveNull2(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add(null);
        list.add("second element");
        list.add(null);
      }
      synchronized (list) {
        list.remove(null);
        list.remove(null);
      }
    }
  }

  void testSubList(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element", "third element",
          "fourth element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("third element");
        list.add("fourth element");
      }
      List subList = list.subList(1, 2);
      ListIterator listIterator = subList.listIterator();
      synchronized (list) {
        listIterator.add("second element");
      }
    }
  }

  void testRemoveAll(List list, boolean validate) {
    if (validate) {
      assertEmptyList(list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
      }
      List removeList = new ArrayList(2);
      removeList.add("first element");
      removeList.add("second element");
      synchronized (list) {
        list.removeAll(removeList);
      }
    }
  }

  void testRemoveRange(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "fourth element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
        list.add("third element");
        list.add("fourth element");
      }
      Class listClass = AbstractList.class;
      Class[] parameterType = new Class[2];
      parameterType[0] = Integer.TYPE;
      parameterType[1] = Integer.TYPE;

      try {
        synchronized (list) {
          Method m = listClass.getDeclaredMethod("removeRange", parameterType);
          m.setAccessible(true); // suppressing java access checking since removeRange is
          // a protected method.
          m.invoke(list, new Object[] { new Integer(1), new Integer(3) });
        }
      } catch (Exception e) {
        // ignore Exception in test.
      }
    }
  }

  void testToArray(List list, boolean validate) {
    Object[] array = getArray(list);

    if (validate) {
      assertListsEqual(Arrays.asList(array), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
      }
      synchronized (array) {
        Object[] returnArray = list.toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }

  // List Iterator testing methods.
  void testListIteratorSet1(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "modified first element", "second element", "third element" }),
                       list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
        list.add("third element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.set("modified first element");
      }
    }
  }

  void testListIteratorSet2(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "modified second element", "third element" }),
                       list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
        list.add("third element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.next();
        lIterator.set("modified second element");
      }
    }
  }

  void testListIteratorSetRemove1(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "modified first element", "third element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
        list.add("third element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.next();
        lIterator.remove();
        lIterator.previous();
        lIterator.set("modified first element");
      }
    }
  }

  void testListIteratorSetRemove2(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "modified second element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
        list.add("third element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.next();
        lIterator.set("modified second element");
        lIterator.next();
        lIterator.remove();
      }
    }
  }

  void testListIteratorDuplicateElementRemove(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
        list.add("first element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.next();
        lIterator.next();
        lIterator.remove();
      }
    }
  }

  void testListIteratorAdd1(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element", "third element" }), list);
      // assertListsEqual(Arrays.asList(new Object[] { "second element", "third element" }), list);
    } else {
      synchronized (list) {
        list.add("second element");
        list.add("third element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.add("first element");
      }
    }
  }

  void testListIteratorAdd2(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element", "third element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("third element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.add("second element");
      }
    }
  }

  void testListIteratorAddSet1(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "modified first element", "second element", "third element" }),
                       list);
    } else {
      synchronized (list) {
        list.add("second element");
        list.add("third element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.add("first element");
        lIterator.previous();
        lIterator.set("modified first element");
      }
    }
  }

  void testListIteratorAddSet2(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element", "modified third element" }),
                       list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("third element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.add("second element");
        lIterator.next();
        lIterator.set("modified third element");
      }
    }
  }

  void testListIteratorAddSet3(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element", "modified third element",
          "fourth element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
        list.add("fourth element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator(1);
        lIterator.next();
        lIterator.add("third element");
        lIterator.previous();
        lIterator.set("modified third element");
      }
    }
  }

  void testListIteratorAddNull(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { null, null, "third element" }), list);
    } else {
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.add(null);
        lIterator.add(null);
        lIterator.add("third element");
      }
    }
  }

  void testListIteratorAddRemove(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "second element", "third element" }), list);
    } else {
      synchronized (list) {
        list.add("second element");
        list.add("third element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.add("first element");
        lIterator.previous();
        lIterator.remove();
      }
    }
  }

  void testListIteratorRemoveNull(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", null, "third element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add(null);
        list.add(null);
        list.add("third element");
      }
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        lIterator.next();
        lIterator.next();
        lIterator.remove();
      }
    }
  }

  // Read only testing methods.
  void testReadOnlyAdd(List list, boolean validate) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertEmptyList(list);
    } else {
      synchronized (list) {
        try {
          list.add("first element");
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException t) {
          // Expected
        }
      }
    }
  }

  void testReadOnlySet(List list, boolean validate) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertEmptyList(list);
    } else {
      synchronized (list) {
        try {
          list.set(0, "first element");
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException t) {
          // Expected
        }
      }
    }
  }

  // Setting up for the ReadOnly test for remove.
  void testSetUpRemove(List list, boolean validate) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
      }
      tryReadOnlyRemove(list);
    }
  }

  // tryReadOnlyRemove() goes hand in hand with testSetUpRemove().
  private void tryReadOnlyRemove(List list) {
    synchronized (list) {
      try {
        list.remove("second element");
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  void testSetUpToArray(List list, boolean validate) {
    if (list instanceof Vector) { return; }

    Object[] array = getArray(list);
    if (validate) {
      assertEmptyObjectArray(array);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
      }
      tryReadOnlyToArray(list);
    }
  }

  void tryReadOnlyToArray(List list) {
    Object[] array = getArray(list);
    synchronized (array) {
      try {
        Object[] returnArray = list.toArray(array);
        Assert.assertTrue(returnArray == array);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for Iterator remove.
  void testSetUpIteratorRemove(List list, boolean validate) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
      }
      tryReadOnlyIteratorRemove(list);
    }
  }

  // tryReadOnlyIteratorRemove() goes hand in hand with testSetUpIteratorRemove().
  private void tryReadOnlyIteratorRemove(List list) {
    synchronized (list) {
      try {
        Iterator iterator = list.iterator();
        iterator.next();
        iterator.remove();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for clear.
  void testSetUpClear(List list, boolean validate) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
      }
      tryReadOnlyClear(list);
    }
  }

  // tryReadOnlyClear() goes hand in hand with testSetUpClear().
  private void tryReadOnlyClear(List list) {
    synchronized (list) {
      try {
        list.clear();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for retainAll.
  void testSetUpRetainAll(List list, boolean validate) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
      }
      tryReadOnlyRetainAll(list);
    }
  }

  // tryReadOnlyRetainAll() goes hand in hand with testSetUpRetainAll().
  private void tryReadOnlyRetainAll(List list) {
    synchronized (list) {
      List toRetain = new ArrayList();
      toRetain.add("first element");
      try {
        list.retainAll(toRetain);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for removeAll.
  void testSetUpRemoveAll(List list, boolean validate) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
      }
      tryReadOnlyRemoveAll(list);
    }
  }

  // tryReadOnlyRemoveAll() goes hand in hand with testSetUpRemoveAll().
  private void tryReadOnlyRemoveAll(List list) {
    synchronized (list) {
      List toRemove = new ArrayList();
      toRemove.add("first element");
      try {
        list.removeAll(toRemove);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException e) {
        // Expected
      }
    }
  }

  void testListIteratorReadOnlyAdd(List list, boolean validate) {
    if (list instanceof Vector) { return; }

    if (validate) {
      assertEmptyList(list);
    } else {
      synchronized (list) {
        ListIterator lIterator = list.listIterator();
        try {
          lIterator.add("first element");
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException e) {
          // Expected
        }
      }
    }
  }

  void testCollectionsAddAll(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element", "third element" }), list);
    } else {
      synchronized (list) {
        list.addAll(Arrays.asList(new Object[] { "first element", "second element", "third element" }));
      }
    }
  }

  // Iterator testing methods.
  void testIteratorRemove(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "second element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
      }
      synchronized (list) {
        Iterator iterator = list.iterator();
        Assert.assertEquals(true, iterator.hasNext());
        iterator.next();
        iterator.remove();
      }
    }
  }

  void testIteratorDuplicateElementRemove(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add("second element");
        list.add("first element");
      }
      synchronized (list) {
        Iterator iterator = list.iterator();
        Assert.assertEquals(true, iterator.hasNext());
        iterator.next();
        iterator.next();
        iterator.next();
        iterator.remove();
      }
    }
  }

  void testIteratorRemoveNull(List list, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", null, "second element" }), list);
    } else {
      synchronized (list) {
        list.add("first element");
        list.add(null);
        list.add(null);
        list.add("second element");
      }
      synchronized (list) {
        Iterator iterator = list.iterator();
        Assert.assertEquals(true, iterator.hasNext());
        iterator.next();
        iterator.next();
        iterator.remove();
      }
    }
  }

  // Stack specific testing method.
  void testStackPush(List list, boolean validate) {
    if (!(list instanceof Stack)) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element", "second element" }), list);
    } else {
      synchronized (list) {
        Stack s = (Stack) list;
        s.push("first element");
        s.push("second element");
      }
    }
  }

  void testStackPop(List list, boolean validate) {
    if (!(list instanceof Stack)) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "first element" }), list);
    } else {
      Stack s = (Stack) list;
      synchronized (list) {
        s.push("first element");
        s.push("second element");
      }
      synchronized (list) {
        Object o = s.pop();
        Assert.assertEquals("second element", o);
      }
    }
  }

  private Object[] getArray(List list) {
    if (list instanceof LinkedList) { return (Object[]) sharedMap.get("arrayforLinkedList"); }
    if (list instanceof ArrayList) { return (Object[]) sharedMap.get("arrayforArrayList"); }
    if (list instanceof Stack) { // need to check instanceof Stack first before checking instance of Vector
      // as Stack is a subclass of Vector.
      return (Object[]) sharedMap.get("arrayforStack");
    }
    if (list instanceof Vector) { return (Object[]) sharedMap.get("arrayforVector"); }
    return null;
  }

  private static void assertEmptyObjectArray(Object[] array) {
    for (int i = 0; i < array.length; i++) {
      Assert.assertNull(array[i]);
    }
  }

  private static void assertEmptyList(List list) {
    Assert.assertEquals(0, list.size());
    Assert.assertTrue(list.isEmpty());

    int count = 0;
    for (Iterator i = list.iterator(); i.hasNext();) {
      count++;
    }
    Assert.assertEquals(0, count);
  }

  private static void assertListsEqual(List expect, List actual) {
    Assert.assertEquals(expect.size(), actual.size());

    Assert.assertTrue(expect.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expect));

    for (int i = 0, n = expect.size(); i < n; i++) {
      Assert.assertEquals(expect.get(i), actual.get(i));
    }

    if (expect.isEmpty()) {
      Assert.assertTrue(actual.isEmpty());
    } else {
      Assert.assertFalse(actual.isEmpty());
    }

    for (Iterator iExpect = expect.iterator(), iActual = actual.iterator(); iExpect.hasNext();) {
      Assert.assertEquals(iExpect.next(), iActual.next());
    }

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = GenericList15TestApp.class.getName();
    config.getOrCreateSpec(testClass);
    String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowedMethodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);
  }

  private static void assertSingleElement(List list, Object obj) {
    Assert.assertEquals(1, list.size());
    Assert.assertEquals(obj, list.get(0));
    Assert.assertFalse(list.isEmpty());
    Assert.assertTrue(list.contains(obj));

    int count = 0;
    for (Iterator i = list.iterator(); i.hasNext();) {
      count++;
      Assert.assertEquals(obj, i.next());
    }
    Assert.assertEquals(1, count);

  }

}
