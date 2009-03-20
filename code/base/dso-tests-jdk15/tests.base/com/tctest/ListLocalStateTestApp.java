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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;

/**
 * Test to make sure local object state is preserved when TC throws: UnlockedSharedObjectException ReadOnlyException
 * TCNonPortableObjectError List version INT-186
 * 
 * @author hhuynh
 */
public class ListLocalStateTestApp extends GenericLocalStateTestApp {
  private List<List> root        = new ArrayList<List>();
  private Class[]    listClasses = new Class[] { ArrayList.class, Vector.class, LinkedList.class, Stack.class };

  public ListLocalStateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    initTest();
    for (LockMode lockMode : LockMode.values()) {
      for (List list : root) {
        testMutate(initList(list), lockMode, AddMutator.class);
        testMutate(initList(list), lockMode, AddAllMutator.class);
        testMutate(initList(list), lockMode, RemoveMutator.class);
        testMutate(initList(list), lockMode, ClearMutator.class);
        testMutate(initList(list), lockMode, RemoveAllMutator.class);
        testMutate(initList(list), lockMode, RetainAllMutator.class);
        testMutate(initList(list), lockMode, IteratorRemoveMutator.class);
        testMutate(initList(list), lockMode, IteratorAddMutator.class);
        testMutate(initList(list), lockMode, ListIteratorRemoveMutator.class);
        // failing - CDV-163
        // testMutate2(initList(list), lockMode, AddAllNonPortableMutator.class);
      }
    }
  }

  private List initList(List l) {
    synchronized (l) {
      l.clear();
      l.add("v1");
      l.add("v2");
      l.add("v3");
      return l;
    }
  }

  private void initTest() throws Exception {
    synchronized (root) {
      for (Class c : listClasses) {
        root.add((List) c.newInstance());
      }
    }
  }

  protected void validate(int before, int after, Object testTarget, LockMode lockMode, Class mutatorClass)
      throws Exception {
    switch (lockMode) {
      case NONE:
      case READ:
        Assert.assertEquals(testTarget, before, after);
        break;
      case WRITE:
        Assert.assertTrue(testTarget, before != after);
        break;
      default:
        throw new RuntimeException("Shouldn't happen");
    }

    if (mutatorClass.equals(AddAllNonPortableMutator.class)) {
      for (Iterator it = ((List) testTarget).iterator(); it.hasNext();) {
        Object o = it.next();
        Assert.assertFalse("Found NonPortable instance: " + testTarget, o instanceof NonPortable);
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ListLocalStateTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");
    config.addIncludePattern(GenericLocalStateTestApp.class.getName() + "$*");
    config.addExcludePattern(NonPortable.class.getName());

    config.addWriteAutolock("* " + testClass + "*.initList(..)");
    config.addWriteAutolock("* " + testClass + "*.initTest()");
    config.addWriteAutolock("* " + testClass + "*.validate(..)");
    config.addReadAutolock("* " + testClass + "*.runTest()");

    spec.addRoot("root", "root");

    config.addReadAutolock("* " + Handler.class.getName() + "*.invokeWithReadLock(..)");
    config.addWriteAutolock("* " + Handler.class.getName() + "*.invokeWithWriteLock(..)");
    config.addWriteAutolock("* " + Handler.class.getName() + "*.setLockMode(..)");
  }

  static class AddMutator implements Mutator {
    public void doMutate(Object o) {
      List l = (List) o;
      l.add("v4");
    }
  }

  static class AddAllMutator implements Mutator {
    public void doMutate(Object o) {
      List l = (List) o;
      List anotherList = new ArrayList();
      anotherList.add("v");
      l.addAll(anotherList);
    }
  }

  static class RemoveMutator implements Mutator {
    public void doMutate(Object o) {
      List l = (List) o;
      l.remove("v1");
    }
  }

  static class ClearMutator implements Mutator {
    public void doMutate(Object o) {
      List l = (List) o;
      l.clear();
    }
  }

  static class RemoveAllMutator implements Mutator {
    public void doMutate(Object o) {
      List l = (List) o;
      List a = new ArrayList();
      a.add("v1");
      a.add("v2");
      l.removeAll(a);
    }
  }

  static class RetainAllMutator implements Mutator {
    public void doMutate(Object o) {
      List l = (List) o;
      List a = new ArrayList();
      a.add("v1");
      a.add("v2");
      l.retainAll(a);
    }
  }

  static class IteratorRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      List l = (List) o;
      for (Iterator it = l.iterator(); it.hasNext();) {
        it.next();
        it.remove();
      }
    }
  }

  static class ListIteratorRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      List l = (List) o;
      for (ListIterator it = l.listIterator(); it.hasNext();) {
        it.next();
        it.remove();
      }
    }
  }

  static class IteratorAddMutator implements Mutator {
    public void doMutate(Object o) {
      List l = (List) o;
      ListIterator it = l.listIterator();
      it.add("v");
    }
  }

  static class AddAllNonPortableMutator implements Mutator {
    public void doMutate(Object o) {
      List l = (List) o;
      List anotherList = new ArrayList();
      anotherList.add("v4");
      anotherList.add("v5");
      anotherList.add(new NonPortable());
      anotherList.add("v6");
      l.addAll(anotherList);
    }
  }
}
