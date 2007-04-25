/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.tx.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import gnu.trove.THashSet;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class GenericSetTestApp extends GenericTestApp {

  public GenericSetTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, Set.class);
  }

  protected Object getTestObject(String testName) {
    Set sets = (Set) sharedMap.get("sets");
    return sets.iterator();
  }

  protected void setupTestObject(String testName) {
    Set sets = new HashSet();
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

  // Generic Set interface testing methods.
  void testBasicAdd(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "January", "February" }), set);
    } else {
      synchronized (set) {
        boolean added = set.add("January");
        Assert.assertTrue(added);
        added = set.add("February");
        Assert.assertTrue(added);
      }
    }
  }

  void testAddAll(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "January", "February" }), set);
    } else {
      Set toAdd = new LinkedHashSet();
      toAdd.add("January");
      toAdd.add("February");
      synchronized (set) {
        boolean added = set.addAll(toAdd);
        Assert.assertTrue(added);
      }
    }
  }

  void testClear(Set set, boolean validate) {
    if (validate) {
      assertEmptySet(set);
    } else {
      Set toAdd = new HashSet();
      toAdd.add("first element");
      toAdd.add("second element");
      synchronized (set) {
        set.clear();
      }
    }
  }

  void testRemove(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "January", "March" }), set);
    } else {
      synchronized (set) {
        set.add("January");
        set.add("February");
        set.add("March");
      }
      synchronized (set) {
        set.remove("February");
      }
    }
  }

  void testRemoveAll(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "January", "April" }), set);
    } else {
      synchronized (set) {
        set.add("January");
        set.add("February");
        set.add("March");
        set.add("April");
      }
      Set toRemove = new HashSet();
      toRemove.add("February");
      toRemove.add("March");
      synchronized (set) {
        set.removeAll(toRemove);
      }
    }
  }

  void testRetainAll(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "February", "March" }), set);
    } else {
      synchronized (set) {
        set.add("January");
        set.add("February");
        set.add("March");
        set.add("April");
      }
      Set toRetain = new HashSet();
      toRetain.add("February");
      toRetain.add("March");
      synchronized (set) {
        set.retainAll(toRetain);
      }
    }
  }

  void testToArray(Set set, boolean validate) {
    Object[] array = getArray(set);

    if (validate) {
      assertSetsEqual(Arrays.asList(array), set);
    } else {
      synchronized (set) {
        set.add("January");
        set.add("February");
      }
      synchronized (array) {
        Object[] returnArray = set.toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }

  // Iterator interface testing methods.
  void testIteratorRemove(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "January", "February" }), set);
    } else {
      synchronized (set) {
        set.add("January");
        set.add("February");
        set.add("March");
      }
      String element;
      synchronized (set) {
        for (Iterator iterator = set.iterator(); iterator.hasNext();) {
          element = (String) iterator.next();
          if ("March".equals(element)) {
            iterator.remove();
          }
        }
      }
    }
  }

  void testIteratorRemoveNull(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "January", "February" }), set);
    } else {
      synchronized (set) {
        set.add("January");
        set.add(null);
        set.add("February");
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
  void testReadOnlyAdd(Set set, boolean validate) {
    if (validate) {
      assertEmptySet(set);
    } else {
      synchronized (set) {
        try {
          set.add("first element");
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException e) {
          // Excepted
        }
      }
    }
  }

  void testReadOnlyAddAll(Set set, boolean validate) {
    if (validate) {
      assertEmptySet(set);
    } else {
      Set toAdd = new HashSet();
      toAdd.add("first element");
      toAdd.add("second element");
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
  void testSetUpRemove(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "January", "February" }), set);
    } else {
      synchronized (set) {
        set.add("January");
        set.add("February");
      }
      tryReadOnlyRemove(set);
    }
  }

  // tryReadOnlyRemove() goes hand in hand with testSetUpRemove().
  private void tryReadOnlyRemove(Set set) {
    synchronized (set) {
      try {
        set.remove("February");
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // expected
      }
    }
  }

  // Setting up for the ReadOnly test for iterator remove.
  void testSetUpIteratorRemove(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "January", "February" }), set);
    } else {
      synchronized (set) {
        set.add("January");
        set.add("February");
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
  void testSetUpClear(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "January", "February" }), set);
    } else {
      synchronized (set) {
        set.add("January");
        set.add("February");
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
  void testSetUpToArray(Set set, boolean validate) {
    if (validate) {
      Object[] array = getArray(set);
      assertEmptyObjectArray(array);
    } else {
      synchronized (set) {
        set.add("January");
        set.add("February");
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
  void testSetUpRetainAll(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "January", "February" }), set);
    } else {
      synchronized (set) {
        set.add("January");
        set.add("February");
      }
      tryReadOnlyRetainAll(set);
    }
  }

  // tryReadOnlyRetainAll() goes hand in hand with testSetUpRetainAll().
  void tryReadOnlyRetainAll(Set set) {
    synchronized (set) {
      Set toRetain = new HashSet();
      toRetain.add("January");
      try {
        set.retainAll(toRetain);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // expected
      }
    }
  }

  // Setting up for the ReadOnly test for RemoveAll.
  void testSetUpRemoveAll(Set set, boolean validate) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { "January", "February" }), set);
    } else {
      synchronized (set) {
        set.add("January");
        set.add("February");
      }
      tryReadOnlyRemoveAll(set);
    }
  }

  // tryReadOnlyRemoveAll() goes hand in hand with testSetUpRemoveAll().
  void tryReadOnlyRemoveAll(Set set) {
    synchronized (set) {
      Set toRemove = new HashSet();
      toRemove.add("January");
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
    Assert.assertEquals(expectElements.size(), actual.size());

    if (actual instanceof LinkedHashSet) {
      for (Iterator iExpect = expectElements.iterator(), iActual = actual.iterator(); iExpect.hasNext();) {
        Assert.assertEquals(iExpect.next(), iActual.next());
      }
    }

    Assert.assertTrue(expectElements.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expectElements));

    if (expectElements.isEmpty()) {
      Assert.assertTrue(actual.isEmpty());
    } else {
      Assert.assertFalse(actual.isEmpty());
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
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
}
