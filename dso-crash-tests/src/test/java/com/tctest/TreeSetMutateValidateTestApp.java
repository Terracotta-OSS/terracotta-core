/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TreeSetMutateValidateTestApp extends AbstractMutateValidateTransparentApp {

  private static final boolean MUTATE   = true;
  private static final boolean VALIDATE = false;

  private final String         myAppId;
  private final Map            myMapOfTreeSets;

  // ROOT
  private Map                  allMaps  = new HashMap();

  public TreeSetMutateValidateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.myAppId = appId;
    myMapOfTreeSets = new HashMap();
  }

  protected void mutate() throws Throwable {
    testAdd(MUTATE, null);
    testAddAll(MUTATE, null);
    testClear(MUTATE, null);
    testRemove(MUTATE, null);
    testFirst(MUTATE, null);
    testLast(MUTATE, null);

    synchronized (allMaps) {
      allMaps.put(myAppId, myMapOfTreeSets);
    }
  }

  protected void validate() throws Throwable {
    synchronized (allMaps) {
      Set appIds = allMaps.keySet();
      for (Iterator iter = appIds.iterator(); iter.hasNext();) {
        String appId = (String) iter.next();
        Map allTreeSets = (Map) allMaps.get(appId);
        testAdd(VALIDATE, allTreeSets);
        testAddAll(VALIDATE, allTreeSets);
        testClear(VALIDATE, allTreeSets);
        testRemove(VALIDATE, allTreeSets);
        testFirst(VALIDATE, allTreeSets);
        testLast(VALIDATE, allTreeSets);
      }
    }
  }

  private TreeSet getPopulatedTreeSet() {
    FooObject fooObject_1 = new FooObject("James", 53, true);
    FooObject fooObject_2 = new FooObject("Susan", 29, true);
    FooObject fooObject_3 = new FooObject("Erin", 87, false);
    TreeSet treeSet = new TreeSet(new NullTolerantComparator());
    Assert.assertTrue(treeSet.add(fooObject_1));
    Assert.assertTrue(treeSet.add(fooObject_2));
    Assert.assertTrue(treeSet.add(fooObject_3));
    return treeSet;
  }

  private void testAdd(boolean mutate, Map allTreeSets) {
    final String key = "testAdd";

    if (mutate) {
      TreeSet myTreeSet = getPopulatedTreeSet();
      myMapOfTreeSets.put(key, myTreeSet);
    } else {
      TreeSet treeSet = (TreeSet) allTreeSets.get(key);
      Assert.assertEquals(treeSet.size(), 3);
      Assert.assertTrue(treeSet.contains(new FooObject("James", 53, true)));
      Assert.assertTrue(treeSet.contains(new FooObject("Susan", 29, true)));
      Assert.assertTrue(treeSet.contains(new FooObject("Erin", 87, false)));
    }
  }

  private void testAddAll(boolean mutate, Map allTreeSets) {
    final String key = "testAddAll";

    if (mutate) {
      TreeSet treeSet = getPopulatedTreeSet();
      TreeSet myTreeSet = new TreeSet(new NullTolerantComparator());
      myTreeSet.addAll(treeSet);
      myMapOfTreeSets.put(key, myTreeSet);
    } else {
      TreeSet treeSet = (TreeSet) allTreeSets.get(key);
      Assert.assertEquals(treeSet.size(), 3);
      Assert.assertTrue(treeSet.contains(new FooObject("James", 53, true)));
      Assert.assertTrue(treeSet.contains(new FooObject("Susan", 29, true)));
      Assert.assertTrue(treeSet.contains(new FooObject("Erin", 87, false)));
    }
  }

  private void testClear(boolean mutate, Map allTreeSets) {
    final String key = "testClear";

    if (mutate) {
      TreeSet myTreeSet = getPopulatedTreeSet();
      myTreeSet.clear();
      myMapOfTreeSets.put(key, myTreeSet);
    } else {
      TreeSet treeSet = (TreeSet) allTreeSets.get(key);
      Assert.assertTrue(treeSet.isEmpty());
    }
  }

  private void testRemove(boolean mutate, Map allTreeSets) {
    final String key = "testRemove";

    if (mutate) {
      TreeSet myTreeSet = getPopulatedTreeSet();
      myTreeSet.remove(new FooObject("Susan", 29, true));
      myMapOfTreeSets.put(key, myTreeSet);
    } else {
      TreeSet treeSet = (TreeSet) allTreeSets.get(key);
      Assert.assertEquals(treeSet.size(), 2);
      Assert.assertTrue(treeSet.contains(new FooObject("James", 53, true)));
      Assert.assertTrue(treeSet.contains(new FooObject("Erin", 87, false)));
    }
  }

  private void testFirst(boolean mutate, Map allTreeSets) {
    final String key = "testFirst";

    if (mutate) {
      TreeSet myTreeSet = getPopulatedTreeSet();
      Assert.assertEquals(myTreeSet.first(), new FooObject("Susan", 29, true));
      myMapOfTreeSets.put(key, myTreeSet);
    } else {
      TreeSet treeSet = (TreeSet) allTreeSets.get(key);
      Assert.assertEquals(treeSet.size(), 3);
      Assert.assertTrue(treeSet.contains(new FooObject("James", 53, true)));
      Assert.assertTrue(treeSet.contains(new FooObject("Susan", 29, true)));
      Assert.assertTrue(treeSet.contains(new FooObject("Erin", 87, false)));
    }
  }

  private void testLast(boolean mutate, Map allTreeSets) {
    final String key = "testLast";

    if (mutate) {
      TreeSet myTreeSet = getPopulatedTreeSet();
      Assert.assertEquals(myTreeSet.last(), new FooObject("Erin", 87, false));
      myMapOfTreeSets.put(key, myTreeSet);
    } else {
      TreeSet treeSet = (TreeSet) allTreeSets.get(key);
      Assert.assertEquals(treeSet.size(), 3);
      Assert.assertTrue(treeSet.contains(new FooObject("James", 53, true)));
      Assert.assertTrue(treeSet.contains(new FooObject("Susan", 29, true)));
      Assert.assertTrue(treeSet.contains(new FooObject("Erin", 87, false)));
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = TreeSetMutateValidateTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("allMaps", "allMaps");
    config.getOrCreateSpec(FooObject.class.getName());
    config.getOrCreateSpec(NullTolerantComparator.class.getName());
  }

  private static final class FooObject implements Comparable {
    private final String  name;
    private final boolean playsBasketball;
    private final int     age;

    public FooObject(String name, int age, boolean playsBasketball) {
      this.name = name;
      this.age = age;
      this.playsBasketball = playsBasketball;
    }

    public String getName() {
      return name;
    }

    public int getAge() {
      return age;
    }

    public boolean playsBasketball() {
      return playsBasketball;
    }

    public boolean equals(Object foo) {
      if (foo == null) { return false; }
      if (((FooObject) foo).getName().equals(name) && ((FooObject) foo).getAge() == age
          && ((FooObject) foo).playsBasketball() == playsBasketball) { return true; }
      return false;
    }

    public int compareTo(Object o) {
      int othersAge = ((FooObject) o).getAge();
      if (age < othersAge) {
        return -1;
      } else if (age == othersAge) {
        return 0;
      } else {
        return 1;
      }
    }
  }

}
