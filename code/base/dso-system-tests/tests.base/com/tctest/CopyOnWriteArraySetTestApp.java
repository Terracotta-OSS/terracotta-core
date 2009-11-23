/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class CopyOnWriteArraySetTestApp extends GenericTransparentApp {

  private static final int LITERAL_VARIANT = 1;
  private static final int OBJECT_VARIANT  = 2;

  public CopyOnWriteArraySetTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, Set.class, 2);
  }

  @Override
  protected Object getTestObject(String testName) {
    List sets = (List) sharedMap.get("sets");
    return sets.iterator();
  }

  @Override
  protected void setupTestObject(String testName) {
    List sets = new ArrayList();
    sets.add(new CopyOnWriteArraySet());

    sharedMap.put("sets", sets);
    sharedMap.put("arrayforCopyOnWriteArraySet", new Object[2]);

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
      boolean added = set.add(E("January", v));
      Assert.assertTrue(added);
      added = set.add(E("February", v));
      Assert.assertTrue(added);
    }
  }

  void testAddAll(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("February", v) }), set);
    } else {
      Set toAdd = new LinkedHashSet();
      toAdd.add(E("January", v));
      toAdd.add(E("February", v));
      boolean added = set.addAll(toAdd);
      Assert.assertTrue(added);
    }
  }

  void testClear(Set set, boolean validate, int v) {
    if (validate) {
      assertEmptySet(set);
    } else {
      Set toAdd = new HashSet();
      toAdd.add(E("first element", v));
      toAdd.add(E("second element", v));
      set.clear();
    }
  }

  void testAddNull(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { null }), set);
    } else {
      System.err.println(set.getClass());
      boolean added = set.add(null);
      if (!added) throw new AssertionError("not added!");
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

      boolean added = set.add(f1);
      if (!added) throw new AssertionError("not added!");

      added = set.add(f2);
      if (added) throw new AssertionError("added!");
    }
  }

  void testRemove(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("March", v) }), set);
    } else {
      set.add(E("January", v));
      set.add(E("February", v));
      set.add(E("March", v));
      boolean remove = set.remove(E("February", v));
      if (!remove) throw new AssertionError("not removed");
    }
  }

  void testRemoveAll(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("January", v), E("April", v) }), set);
    } else {
      set.add(E("January", v));
      set.add(E("February", v));
      set.add(E("March", v));
      set.add(E("April", v));
      Set toRemove = new HashSet();
      toRemove.add(E("February", v));
      toRemove.add(E("March", v));
      set.removeAll(toRemove);
    }
  }

  void testRetainAll(Set set, boolean validate, int v) {
    if (validate) {
      assertSetsEqual(Arrays.asList(new Object[] { E("February", v), E("March", v) }), set);
    } else {
      set.add(E("January", v));
      set.add(E("February", v));
      set.add(E("March", v));
      set.add(E("April", v));
      Set toRetain = new HashSet();
      toRetain.add(E("February", v));
      toRetain.add(E("March", v));
      set.retainAll(toRetain);
    }
  }

  void testToArray(Set set, boolean validate, int v) {
    Object[] array = getArray(set);

    if (validate) {
      assertSetsEqual(Arrays.asList(array), set);
    } else {
      set.add(E("January", v));
      set.add(E("February", v));
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
      set.add(E("January", v));

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

  private Object[] getArray(Set set) {
    return (Object[]) sharedMap.get("arrayforCopyOnWriteArraySet");

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
    String testClass = CopyOnWriteArraySetTestApp.class.getName();
    config.addIncludePattern(testClass + "$*");
    config.getOrCreateSpec(testClass);
    String toArrayMethodExpression = "* " + testClass + "*.*ToArray*(..)";
    config.addWriteAutolock(toArrayMethodExpression);
  }

  private static class Foo implements Comparable {
    private final String value;

    // This field isn't used in equals/hashCode -- It is only here to test which instance is retained when
    // two unique (but equal) objects are added to a set
    private String       id;

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
