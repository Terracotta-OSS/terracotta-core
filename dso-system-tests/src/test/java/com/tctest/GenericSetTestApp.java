/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import gnu.trove.THashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class GenericSetTestApp extends GenericTransparentApp {

  private static final int LITERAL_VARIANT = 1;
  private static final int OBJECT_VARIANT  = 2;

  public GenericSetTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, Set.class, 2);
  }

  protected Object getTestObject(String testName) {
    List sets = (List) sharedMap.get("sets");
    return sets.iterator();
  }

  protected void setupTestObject(String testName) {
    List sets = new ArrayList();
    sets.add(new HashSet());
    sets.add(new LinkedHashSet());
    sets.add(new THashSet());
    sets.add(new TreeSet(new NullTolerantComparator()));
    sets.add(new MyHashSet());
    sets.add(new MyLinkedHashSet());
    sets.add(new MyTHashSet());
    sets.add(new MyTreeSet(new NullTolerantComparator()));
    // sets.add(new SubclassOfAbstractLogicalSubclass());

    sharedMap.put("sets", sets);
    sharedMap.put("arrayforHashSet", new Object[2]);
    sharedMap.put("arrayforTHashSet", new Object[2]);
    sharedMap.put("arrayforTreeSet", new Object[2]);
    sharedMap.put("arrayforLinkedHashSet", new Object[2]);
    sharedMap.put("arrayforMyHashSet", new Object[2]);
    sharedMap.put("arrayforMyTHashSet", new Object[2]);
    sharedMap.put("arrayforMyTreeSet", new Object[2]);
    sharedMap.put("arrayforMyLinkedHashSet", new Object[2]);
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

  // Generic Set interface testing methods.
  void testBasicAdd(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("February", v) }), set);
    } else {
      synchronized (set) {
        boolean added = set.add(E("January", v));
        Assert.assertTrue(added);
        added = set.add(E("February", v));
        Assert.assertTrue(added);
      }
    }
  }

  void testAddAll(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("February", v) }), set);
    } else {
      Set toAdd = new LinkedHashSet();
      toAdd.add(E("January", v));
      toAdd.add(E("February", v));
      synchronized (set) {
        boolean added = set.addAll(toAdd);
        Assert.assertTrue(added);
      }
    }
  }

  void testClear(Set set, boolean validate, int v) {
    if (validate) {
      assertEmptySet(set);
    } else {
      Set toAdd = new HashSet();
      toAdd.add(E("first element", v));
      toAdd.add(E("second element", v));
      synchronized (set) {
        set.clear();
      }
    }
  }

  void testAddNull(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { null }), set);
    } else {
      System.err.println(set.getClass());
      synchronized (set) {
        boolean added = set.add(null);
        if (!added) throw new AssertionError("not added!");
      }
    }
  }

  void testElementRetention(Set set, boolean validate, int v) {
    if (v == LITERAL_VARIANT) return;

    if (validate) {
      Assert.assertEquals(1, set.size());
      Foo f = (Foo) set.iterator().next();
      Assert.assertEquals(set.getClass().getName(), "id1", f.id);
    } else {
      Foo f1 = (Foo) E("1", v);
      f1.id = "id1";
      Foo f2 = (Foo) E("1", v);
      f2.id = "id2";

      synchronized (set) {
        boolean added = set.add(f1);
        if (!added) throw new AssertionError("not added!");
      }

      synchronized (set) {
        boolean added = set.add(f2);
        if (added) throw new AssertionError("added!");
      }
    }
  }

  void testRemove(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("March", v) }), set);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(E("February", v));
        set.add(E("March", v));
      }
      synchronized (set) {
        boolean remove = set.remove(E("February", v));
        if (!remove) throw new AssertionError("not removed");
      }
    }
  }

  void testRemoveAll(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("April", v) }), set);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(E("February", v));
        set.add(E("March", v));
        set.add(E("April", v));
      }
      Set toRemove = new HashSet();
      toRemove.add(E("February", v));
      toRemove.add(E("March", v));
      synchronized (set) {
        set.removeAll(toRemove);
      }
    }
  }

  void testRetainAll(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("February", v), E("March", v) }), set);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(E("February", v));
        set.add(E("March", v));
        set.add(E("April", v));
      }
      Set toRetain = new HashSet();
      toRetain.add(E("February", v));
      toRetain.add(E("March", v));
      synchronized (set) {
        set.retainAll(toRetain);
      }
    }
  }

  void testToArray(Set set, boolean validate, int v) {
    Object[] array = getArray(set);

    if (validate) {
      assertSetsEqual(Arrays.asList(array), set);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(E("February", v));
      }
      synchronized (array) {
        Object[] returnArray = set.toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }

  void testToArray2(Set set, boolean validate, int v) {
    Object[] array = getArray(set);

    if (validate) {
      Assert.assertEquals(set.getClass(), E("January", v), array[0]);
      Assert.assertEquals(set.getClass(), null, array[1]);
    } else {
      synchronized (set) {
        set.add(E("January", v));
      }

      // ensure that the array is bigger than the set size
      // This test case makes sure the array get's null terminated
      Assert.assertEquals(1, set.size());
      Assert.assertEquals(2, array.length);

      // make sure the array contains no nulls
      synchronized (array) {
        Arrays.fill(array, new Object());
      }

      synchronized (array) {
        Object[] returnArray = set.toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }

  // Iterator interface testing methods.
  void testIteratorRemove(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("February", v) }), set);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(E("February", v));
        set.add(E("March", v));
      }

      synchronized (set) {
        for (Iterator iterator = set.iterator(); iterator.hasNext();) {
          if (E("March", v).equals(iterator.next())) {
            iterator.remove();
          }
        }
      }
    }
  }

  void testIteratorRemoveNull(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("February", v) }), set);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(null);
        set.add(E("February", v));
      }
      synchronized (set) {
        Iterator iterator = set.iterator();
        Assert.assertEquals(true, iterator.hasNext());
        while (iterator.hasNext()) {
          Object o = iterator.next();
          if (o == null) {
            iterator.remove();
          }
        }
      }
    }
  }

  // ReadOnly testing methods.
  void testReadOnlyAdd(Set set, boolean validate, int v) {
    if (validate) {
      assertEmptySet(set);
    } else {
      synchronized (set) {
        try {
          set.add(E("first element", v));
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException e) {
          // Excepted
        }
      }
    }
  }

  void testReadOnlyAddAll(Set set, boolean validate, int v) {
    if (validate) {
      assertEmptySet(set);
    } else {
      Set toAdd = new HashSet();
      toAdd.add(E("first element", v));
      toAdd.add(E("second element", v));
      synchronized (set) {
        try {
          set.addAll(toAdd);
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException r) {
          // Expected
        }
      }
    }
  }

  // Setting up for the ReadOnly test for remove.
  void testSetUpRemove(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("February", v) }), set);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(E("February", v));
      }
      tryReadOnlyRemove(set, v);
    }
  }

  // tryReadOnlyRemove() goes hand in hand with testSetUpRemove().
  private void tryReadOnlyRemove(Set set, int v) {
    synchronized (set) {
      try {
        set.remove(E("February", v));
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // expected
      }
    }
  }

  // Setting up for the ReadOnly test for iterator remove.
  void testSetUpIteratorRemove(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("February", v) }), set);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(E("February", v));
      }
      tryReadOnlyIteratorRemove(set);
    }
  }

  // tryReadOnlyIteratorRemove() goes hand in hand with testSetUpIteratorRemove().
  private void tryReadOnlyIteratorRemove(Set set) {
    synchronized (set) {
      try {
        Iterator iterator = set.iterator();
        iterator.next();
        iterator.remove();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException r) {
        // Read Only Exception is expected
      }
    }
  }

  // Setting up for the ReadOnly test for clear.
  void testSetUpClear(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("February", v) }), set);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(E("February", v));
      }
      tryReadOnlyClear(set);
    }
  }

  // tryReadOnlyClear() goes hand in hand with testSetUpClear().
  private void tryReadOnlyClear(Set set) {
    synchronized (set) {
      try {
        set.clear();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Excepted
      }
    }
  }

  // Setting up for the ReadOnly test for toArray.
  void testSetUpToArray(Set set, boolean validate, int v) {
    if (validate) {
      Object[] array = getArray(set);
      assertEmptyObjectArray(array);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(E("February", v));
      }
      tryReadOnlyToArray(set);
    }
  }

  // tryReadOnlyToArray() goes hand in hand with testSetUpToArray().
  void tryReadOnlyToArray(Set set) {
    Object[] array = getArray(set);
    synchronized (array) {
      try {
        Object[] returnArray = set.toArray(array);
        Assert.assertTrue(returnArray == array);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Excepted
      }
    }
  }

  // Setting up for the ReadOnly test for RetainAll.
  void testSetUpRetainAll(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("February", v) }), set);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(E("February", v));
      }
      tryReadOnlyRetainAll(set, v);
    }
  }

  // tryReadOnlyRetainAll() goes hand in hand with testSetUpRetainAll().
  void tryReadOnlyRetainAll(Set set, int v) {
    synchronized (set) {
      Set toRetain = new HashSet();
      toRetain.add(E("January", v));
      try {
        set.retainAll(toRetain);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // expected
      }
    }
  }

  // Setting up for the ReadOnly test for RemoveAll.
  void testSetUpRemoveAll(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("February", v) }), set);
    } else {
      synchronized (set) {
        set.add(E("January", v));
        set.add(E("February", v));
      }
      tryReadOnlyRemoveAll(set, v);
    }
  }

  // tryReadOnlyRemoveAll() goes hand in hand with testSetUpRemoveAll().
  void tryReadOnlyRemoveAll(Set set, int v) {
    synchronized (set) {
      Set toRemove = new HashSet();
      toRemove.add(E("January", v));
      try {
        set.removeAll(toRemove);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Excepted
      }
    }
  }

  private Object getMySubclassArray(Set set) {
    if (set instanceof MyLinkedHashSet) { return sharedMap.get("arrayforMyLinkedHashSet"); }
    if (set instanceof MyHashSet) { return sharedMap.get("arrayforMyHashSet"); }
    if (set instanceof MyTHashSet) { return sharedMap.get("arrayforMyTHashSet"); }
    if (set instanceof MyTreeSet) { return sharedMap.get("arrayforMyTreeSet"); }
    return null;
  }

  private Object[] getArray(Set set) {
    Object o = getMySubclassArray(set);
    if (o != null) { return (Object[]) o; }

    if (set instanceof LinkedHashSet) { return (Object[]) sharedMap.get("arrayforLinkedHashSet"); }
    if (set instanceof HashSet) { return (Object[]) sharedMap.get("arrayforHashSet"); }
    if (set instanceof THashSet) { return (Object[]) sharedMap.get("arrayforTHashSet"); }
    if (set instanceof TreeSet) { return (Object[]) sharedMap.get("arrayforTreeSet"); }
    return null;
  }

  private static void assertEmptyObjectArray(Object[] array) {
    for (int i = 0; i < array.length; i++) {
      Assert.assertNull(array[i]);
    }
  }

  private static void assertEmptySet(Set set) {
    Assert.assertEquals(0, set.size());
    Assert.assertTrue(set.isEmpty());

    int count = 0;
    for (Iterator i = set.iterator(); i.hasNext();) {
      count++;
    }
    Assert.assertEquals(0, count);
  }

  private static void assertSetsEqual(List expectElements, Set actual) {
    String type = actual.getClass().getName();

    Assert.assertEquals(type, expectElements.size(), actual.size());

    if (actual instanceof LinkedHashSet) {
      for (Iterator iExpect = expectElements.iterator(), iActual = actual.iterator(); iExpect.hasNext();) {
        Assert.assertEquals(type, iExpect.next(), iActual.next());
      }
    }

    Assert.assertTrue(type, expectElements.containsAll(actual));
    Assert.assertTrue(type, actual.containsAll(expectElements));

    if (expectElements.isEmpty()) {
      Assert.assertTrue(type, actual.isEmpty());
    } else {
      Assert.assertFalse(type, actual.isEmpty());
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.getOrCreateSpec(NullTolerantComparator.class.getName());
    String testClass = GenericSetTestApp.class.getName();
    config.addIncludePattern(testClass + "$*");
    config.getOrCreateSpec(testClass);
    String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowedMethodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);
  }

  private static class MyHashSet extends HashSet {
    public MyHashSet() {
      super();
    }
  }

  private static class MyLinkedHashSet extends LinkedHashSet {
    public MyLinkedHashSet() {
      super();
    }
  }

  private static class MyTHashSet extends THashSet {
    public MyTHashSet() {
      super();
    }
  }

  private static class MyTreeSet extends TreeSet {
    public MyTreeSet(Comparator comparator) {
      super(comparator);
    }
  }

  private static class Foo implements Comparable {
    private final String value;

    // This field isn't used in equals/hashCode -- It is only here to test which instance is retained when
    // two unique (but equal) objects are added to a set
    private String       id;

    Foo(String value) {
      this.value = value;
    }

    public int hashCode() {
      return value.hashCode();
    }

    public boolean equals(Object obj) {
      if (obj instanceof Foo) { return value.equals(((Foo) obj).value); }
      return false;
    }

    public int compareTo(Object o) {
      return value.compareTo(((Foo) o).value);
    }
  }

}
