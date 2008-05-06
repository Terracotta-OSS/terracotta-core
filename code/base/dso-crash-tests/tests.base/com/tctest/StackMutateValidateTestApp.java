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
import java.util.Stack;

public class StackMutateValidateTestApp extends AbstractMutateValidateTransparentApp {
  private static final boolean MUTATE   = true;
  private static final boolean VALIDATE = false;

  private final String         myAppId;
  private final Map            myMapOfStacks;

  // ROOT
  private Map                  allMaps  = new HashMap();

  public StackMutateValidateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.myAppId = appId;
    myMapOfStacks = new HashMap();
  }

  protected void mutate() throws Throwable {
    testEmpty(MUTATE, null);
    testPush(MUTATE, null);
    testPeek(MUTATE, null);
    testPop(MUTATE, null);
    testSearch(MUTATE, null);

    synchronized (allMaps) {
      allMaps.put(myAppId, myMapOfStacks);
    }
  }

  protected void validate() throws Throwable {
    synchronized (allMaps) {
      Set appIds = allMaps.keySet();
      for (Iterator iter = appIds.iterator(); iter.hasNext();) {
        String appId = (String) iter.next();
        Map allStacks = (Map) allMaps.get(appId);
        testEmpty(VALIDATE, allStacks);
        testPush(VALIDATE, allStacks);
        testPeek(VALIDATE, allStacks);
        testPop(VALIDATE, allStacks);
        testSearch(VALIDATE, allStacks);
      }
    }
  }

  private void testEmpty(boolean mutate, Map allStacks) {
    final String key = "testEmpty";

    if (mutate) {
      Stack myStack = new Stack();
      Assert.assertTrue(myStack.add(new FooObject("James", 53, true)));
      Assert.assertTrue(myStack.remove(new FooObject("James", 53, true)));
      myMapOfStacks.put(key, myStack);
    } else {
      Stack stack = (Stack) allStacks.get(key);
      Assert.assertTrue(stack.isEmpty());
    }
  }

  private Stack getPopulatedStack() {
    FooObject fooObject_1 = new FooObject("James", 53, true);
    FooObject fooObject_2 = new FooObject("Susan", 29, true);
    FooObject fooObject_3 = new FooObject("Erin", 87, false);
    Stack stack = new Stack();
    stack.push(fooObject_1);
    stack.push(fooObject_2);
    stack.push(fooObject_3);
    return stack;
  }

  private void testPush(boolean mutate, Map allStacks) {
    final String key = "testPush";

    if (mutate) {
      Stack myStack = getPopulatedStack();
      myMapOfStacks.put(key, myStack);
    } else {
      Stack stack = (Stack) allStacks.get(key);
      Assert.assertEquals(stack.search(new FooObject("James", 53, true)), 3);
      Assert.assertEquals(stack.search(new FooObject("Susan", 29, true)), 2);
      Assert.assertEquals(stack.search(new FooObject("Erin", 87, false)), 1);
    }
  }

  private void testPeek(boolean mutate, Map allStacks) {
    final String key = "testPeek";

    if (mutate) {
      Stack myStack = getPopulatedStack();
      Assert.assertEquals(myStack.peek(), new FooObject("Erin", 87, false));
      myMapOfStacks.put(key, myStack);
    } else {
      Stack stack = (Stack) allStacks.get(key);
      Assert.assertEquals(stack.search(new FooObject("James", 53, true)), 3);
      Assert.assertEquals(stack.search(new FooObject("Susan", 29, true)), 2);
      Assert.assertEquals(stack.search(new FooObject("Erin", 87, false)), 1);
    }
  }

  private void testPop(boolean mutate, Map allStacks) {
    final String key = "testPop";

    if (mutate) {
      Stack myStack = getPopulatedStack();
      Assert.assertEquals(myStack.pop(), new FooObject("Erin", 87, false));
      myMapOfStacks.put(key, myStack);
    } else {
      Stack stack = (Stack) allStacks.get(key);
      Assert.assertEquals(stack.search(new FooObject("James", 53, true)), 2);
      Assert.assertEquals(stack.search(new FooObject("Susan", 29, true)), 1);
      Assert.assertEquals(stack.search(new FooObject("Erin", 87, false)), -1);
    }
  }

  private void testSearch(boolean mutate, Map allStacks) {
    final String key = "testSearch";

    if (mutate) {
      Stack myStack = getPopulatedStack();
      Assert.assertEquals(myStack.search(new FooObject("James", 53, true)), 3);
      Assert.assertEquals(myStack.search(new FooObject("Susan", 29, true)), 2);
      Assert.assertEquals(myStack.search(new FooObject("Erin", 87, false)), 1);
      myMapOfStacks.put(key, myStack);
    } else {
      Stack stack = (Stack) allStacks.get(key);
      Assert.assertEquals(stack.search(new FooObject("James", 53, true)), 3);
      Assert.assertEquals(stack.search(new FooObject("Susan", 29, true)), 2);
      Assert.assertEquals(stack.search(new FooObject("Erin", 87, false)), 1);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = StackMutateValidateTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("allMaps", "allMaps");
    config.getOrCreateSpec(FooObject.class.getName());
  }

  private static final class FooObject {
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
  }

}
