/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.GenericTransparentApp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class CopyOnWriteArrayTestApp extends GenericTransparentApp {

  private static final int LITERAL_VARIANT = 1;
  private static final int OBJECT_VARIANT  = 2;

  public CopyOnWriteArrayTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, List.class, 2);
  }

  @Override
  protected Object getTestObject(String testName) {
    List lists = (List) sharedMap.get("lists");
    return lists.iterator();
  }

  // This method is kind of like a macro, it returns an element (E == element) to be used
  // in the set based on the variant value
  Object E(String val, int variant) {
    switch (variant) {
      case LITERAL_VARIANT: {
        return val;
      }
      case OBJECT_VARIANT: {
        return new Foo(val);
      }
      default: {
        throw new AssertionError("unknown variant: " + variant);
      }
    }

    // unreachable
  }

  @Override
  protected void setupTestObject(String testName) {
    List lists = new ArrayList();
    lists.add(new CopyOnWriteArrayList());

    sharedMap.put("lists", lists);
    sharedMap.put("arrayforCOWArrayList", new Object[2]);
  }

  void testBasicAdd(List list, boolean validate, int v) {
    if (validate) {
      assertSingleElement(list, E("rollin in my 6-4", v));
    } else {
      synchronized (list) {
        boolean added = list.add(E("rollin in my 6-4", v));
        Assert.assertTrue(added);
      }
    }
  }

  void testAddIfAbsent(List list, boolean validate, int v) {
    if (!(list instanceof CopyOnWriteArrayList)) return;
    CopyOnWriteArrayList cowList = (CopyOnWriteArrayList) list;
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("one", v), E("two", v), E("three", v) }), cowList);
    } else {
      boolean added = cowList.addIfAbsent(E("one", v));
      Assert.assertTrue(added);
      added = cowList.addIfAbsent(E("two", v));
      Assert.assertTrue(added);
      added = cowList.addIfAbsent(E("three", v));
      Assert.assertTrue(added);
      added = cowList.addIfAbsent(E("two", v));
      Assert.assertFalse(added);
    }
  }

  void testAddAllIfAbsent(List list, boolean validate, int v) {
    if (!(list instanceof CopyOnWriteArrayList)) return;
    CopyOnWriteArrayList cowList = (CopyOnWriteArrayList) list;
    if (validate) {
      assertListsEqual(Arrays
          .asList(new Object[] { E("one", v), E("two", v), E("three", v), E("four", v), E("five", v) }), cowList);
    } else {
      List extra = new ArrayList();
      extra.add(E("two", v));
      extra.add(E("four", v));
      extra.add(E("one", v));
      extra.add(E("five", v));

      cowList.add(E("one", v));
      cowList.add(E("two", v));
      cowList.add(E("three", v));
      int addCount = cowList.addAllAbsent(extra);
      Assert.assertEquals(2, addCount);
    }
  }

  void testBasicRemove(List list, boolean validate, int v) {
    if (validate) {
      assertListsEqual(Arrays.asList(E("two", v)), list);
    } else {
      synchronized (list) {
        boolean added = list.add(E("one", v));
        Assert.assertTrue(added);
        added = list.add(E("two", v));
        Assert.assertTrue(added);
        added = list.add(E("three", v));
        Assert.assertTrue(added);
      }

      boolean removed = list.remove(E("one", v));
      Assert.assertTrue(removed);
      removed = list.remove(E("three", v));
      Assert.assertTrue(removed);
    }
  }

  void testBasicAddNull(List list, boolean validate, int v) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { null, null, E("my cat hates you", v), null }), list);
    } else {
      boolean added;
      added = list.add(null);
      Assert.assertTrue(added);
      added = list.add(null);
      Assert.assertTrue(added);
      added = list.add(E("my cat hates you", v));
      Assert.assertTrue(added);
      added = list.add(null);
      Assert.assertTrue(added);
    }
  }

  void testBasicAddAt(List list, boolean validate, int v) {
    if (validate) {
      assertListsEqual(Arrays.asList(E("1", v), E("2", v), E("3", v), E("4", v)), list);
    } else {
      list.add(0, E("2", v));
      list.add(0, E("1", v));
      list.add(2, E("4", v));
      list.add(2, E("3", v));
    }
  }

  void testAdd(List list, boolean validate, int v) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("element", v) }), list);
    } else {
      list.add(E("element", v));
    }
  }

  void testAddAll(List list, boolean validate, int v) {
    if (validate) {
      assertListsEqual(Arrays.asList(E("patty", v), E("calahan", v), E("was", v), E("here", v)), list);
    } else {
      List toAdd = new ArrayList();
      toAdd.add(E("patty", v));
      toAdd.add(E("calahan", v));
      toAdd.add(E("was", v));
      toAdd.add(E("here", v));
      list.addAll(toAdd);
    }
  }

  void testAddAllAt(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(E("uno", v), E("dos", v), E("tres", v), E("catorce?", v)), list);
    } else {
      list.add(E("uno", v));

      List toAdd = new ArrayList();
      toAdd.add(E("dos", v));
      toAdd.add(E("tres", v));
      toAdd.add(E("catorce?", v));

      list.addAll(1, toAdd);
    }
  }

  void testClear(List list, boolean validate, int v) {

    if (validate) {
      assertEmptyList(list);
    } else {
      list.add(E("clear me baby", v));
      list.clear();

      list.add(E("clear me baby one more time", v));

      list.clear();
    }
  }

  void testSetElementAt(List list, boolean validate, int v) {
    if (validate) {
      assertSingleElement(list, E("new", v));
    } else {
      list.add(E("orig", v));
      // System.out.println("list content before set: " + list);
      list.set(0, E("new", v));
      // System.out.println("list content after set: " + list);
    }
  }

  void testRemoveAt(List list, boolean validate, int v) {

    if (validate) {
      Object item0 = list.get(0);
      Object item1 = list.get(1);
      Assert.assertEquals(E("value", v), item0);
      Assert.assertEquals(E("different value", v), item1);
    } else {
      list.add(E("value", v));
      list.add(E("different value", v));
      list.add(E("value", v));

      Object prev = list.remove(2);
      Assert.assertEquals(E("value", v), prev);
    }
  }

  void testRemoveNull1(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), null, E("third element", v) }), list);
    } else {
      list.add(E("first element", v));
      list.add(null);
      list.add(null);
      list.add(E("third element", v));
      list.remove(null);
    }
  }

  void testRemoveNull2(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("second element", v) }), list);
    } else {
      list.add(E("first element", v));
      list.add(null);
      list.add(E("second element", v));
      list.add(null);
      list.remove(null);
      list.remove(null);
    }
  }

  void testSubList(List list, boolean validate, int v) {
    if (validate) {
      assertListsEqual(Arrays.asList(E("first element", v), E("third element", v), E("fourth element", v)), list);
    } else {
      list.add(E("first element", v));
      list.add(E("third element", v));
      list.add(E("fourth element", v));
      List subList = list.subList(1, 2);
      ListIterator listIterator = subList.listIterator();
      Assert.assertEquals(E("third element", v), listIterator.next());
      Assert.assertFalse(listIterator.hasNext());
    }
  }

  void testRemoveAll(List list, boolean validate, int v) {

    if (validate) {
      assertEmptyList(list);
    } else {
      list.add(E("first element", v));
      list.add(E("second element", v));
      List removeList = new ArrayList(2);
      removeList.add(E("first element", v));
      removeList.add(E("second element", v));
      list.removeAll(removeList);
    }
  }

  void testRemoveRange(List list, boolean validate, int v) throws SecurityException, NoSuchMethodException,
      IllegalArgumentException, IllegalAccessException, InvocationTargetException {

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("fourth element", v) }), list);
    } else {
      synchronized (list) {
        list.add(E("first element", v));
        list.add(E("second element", v));
        list.add(E("third element", v));
        list.add(E("fourth element", v));
      }
      Class listClass = CopyOnWriteArrayList.class;
      Class[] parameterType = new Class[2];
      parameterType[0] = Integer.TYPE;
      parameterType[1] = Integer.TYPE;

      Method m = listClass.getDeclaredMethod("removeRange", parameterType);
      m.setAccessible(true); // suppressing java access checking since removeRange is
      // a protected method.
      m.invoke(list, new Object[] { new Integer(1), new Integer(3) });
    }
  }

  void testRetainAll(List list, boolean validate, int v) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { E("first element", v), E("third element", v) }), list);
    } else {
      list.add(E("first element", v));
      list.add(E("second element", v));
      list.add(E("third element", v));
      list.add(E("fourth element", v));

      List retainList = new ArrayList(2);
      retainList.add(E("first element", v));
      retainList.add(E("third element", v));
      list.retainAll(retainList);
    }
  }

  void testToArray(List list, boolean validate, int v) {

    Object[] array = getArray(list);

    if (validate) {
      assertListsEqual(Arrays.asList(array), list);
    } else {
      list.add(E("first element", v));
      list.add(E("second element", v));
      synchronized (array) {
        Object[] returnArray = list.toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }

  void testToArray2(List list, boolean validate, int v) {
    Object[] array = getArray(list);

    if (validate) {
      Assert.assertEquals(list.getClass(), E("January", v), array[0]);
      Assert.assertEquals(list.getClass(), null, array[1]);
    } else {
      list.add(E("January", v));

      // ensure that the array is bigger than the list size
      // This test case makes sure the array get's null terminated
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(2, array.length);

      // make sure the array contains no nulls
      synchronized (array) {
        Arrays.fill(array, new Object());
      }

      synchronized (array) {
        Object[] returnArray = list.toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }

  void testListIteratorSet1(List list, boolean validate, int v) {
    if (validate) {
      assertListsEqual(Arrays.asList(E("first", v), E("second", v), E("third", v)), list);
    } else {
      list.add(E("first", v));
      list.add(E("second", v));
      list.add(E("third", v));
      ListIterator lIterator = list.listIterator();
      lIterator.next();
      try {
        lIterator.set(E("modified first element", v));
        Assert.fail("Expecting UnsupportedOperationException");
      } catch (UnsupportedOperationException uoe) {
        // expected
      }
    }
  }

  void testListIteratorSetRemove1(List list, boolean validate, int v) {
    if (validate) {
      assertListsEqual(Arrays.asList(E("first", v), E("second", v), E("third", v)), list);
    } else {
      list.add(E("first", v));
      list.add(E("second", v));
      list.add(E("third", v));
      ListIterator lIterator = list.listIterator();
      lIterator.next();
      lIterator.next();
      try {
        lIterator.remove();
        Assert.fail("Expecting UnsupportedOperationException");
      } catch (UnsupportedOperationException uoe) {
        // expected
      }
    }
  }

  void testListIteratorAdd1(List list, boolean validate, int v) {
    if (validate) {
      assertListsEqual(Arrays.asList(E("first", v), E("second", v), E("third", v)), list);
    } else {
      list.add(E("first", v));
      list.add(E("second", v));
      list.add(E("third", v));
      ListIterator lIterator = list.listIterator();
      try {
        lIterator.add("fourth");
        Assert.fail("Expecting UnsupportedOperationException");
      } catch (UnsupportedOperationException uoe) {
        // expected
      }
    }
  }

  void testCollectionsAddAll(List list, boolean validate, int v) {

    if (validate) {
      assertListsEqual(Arrays.asList(E("first element", v), E("second element", v), E("third element", v)), list);
    } else {
      list.addAll(Arrays.asList(E("first element", v), E("second element", v), E("third element", v)));
    }
  }

  // Iterator testing methods.
  void testIteratorRemove(List list, boolean validate, int v) {
    if (validate) {
      assertListsEqual(Arrays.asList(E("first", v), E("second", v)), list);
    } else {
      list.add(E("first", v));
      list.add(E("second", v));
      Iterator iterator = list.iterator();
      Assert.assertEquals(true, iterator.hasNext());
      iterator.next();
      try {
        iterator.remove();
        Assert.fail("Expecting UnsupportedOperationException");
      } catch (UnsupportedOperationException uoe) {
        // expected
      }
    }
  }

  void testAddNonPortableObject(List list, boolean validate, int v) {
    if (!validate) {
      try {
        list.add(new Thread());
        throw new AssertionError("Should have thrown a TCNonPortableObjectError.");
      } catch (TCNonPortableObjectError e) {
        // expected
      }
    }
  }

  private Object[] getArray(List list) {
    return (Object[]) sharedMap.get("arrayforCOWArrayList");
  }

  private static void assertEmptyList(List list) {
    Assert.assertEquals(list.getClass(), 0, list.size());
    Assert.assertTrue(list.getClass(), list.isEmpty());

    int count = 0;
    for (Iterator i = list.iterator(); i.hasNext();) {
      count++;
    }

    Assert.assertEquals(list.getClass(), 0, count);

    for (Iterator i = list.listIterator(); i.hasNext();) {
      count++;
    }

    Assert.assertEquals(list.getClass(), 0, count);
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
    String testClass = CopyOnWriteArrayTestApp.class.getName();
    config.getOrCreateSpec(testClass);
    String writeMethodExpression = "* " + testClass + "*.*ToArray*(..)";
    config.addWriteAutolock(writeMethodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);
    config.addIncludePattern(testClass + "$*");
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

  private static class Foo implements Comparable {
    private final String value;

    Foo(String value) {
      this.value = value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Foo) { return value.equals(((Foo) obj).value); }
      return false;
    }

    public int compareTo(Object o) {
      return value.compareTo(((Foo) o).value);
    }
  }

}
