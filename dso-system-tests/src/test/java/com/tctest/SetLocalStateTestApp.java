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

import gnu.trove.THashSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Test to make sure local object state is preserved when TC throws: UnlockedSharedObjectException ReadOnlyException
 * TCNonPortableObjectError Set version INT-186
 * 
 * @author hhuynh
 */
public class SetLocalStateTestApp extends GenericLocalStateTestApp {
  private List<Set> root       = new ArrayList<Set>();
  private Class[]   setClasses = new Class[] { HashSet.class, TreeSet.class, LinkedHashSet.class, THashSet.class };

  public SetLocalStateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    initTest();

    for (LockMode lockMode : LockMode.values()) {
      for (Set set : root) {
        testMutate(initSet(set), lockMode, AddMutator.class);
        testMutate(initSet(set), lockMode, AddAllMutator.class);
        testMutate(initSet(set), lockMode, RemoveMutator.class);
        testMutate(initSet(set), lockMode, ClearMutator.class);
        testMutate(initSet(set), lockMode, RemoveAllMutator.class);
        testMutate(initSet(set), lockMode, RetainAllMutator.class);
        testMutate(initSet(set), lockMode, IteratorRemoveMutator.class);

        // failing - CDV-163
        // testMutate(initSet(set), lockMode, AddAllNonPortableMutator.class);
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
      for (Iterator it = ((Set) testTarget).iterator(); it.hasNext();) {
        Object o = it.next();
        Assert.assertFalse("Found NonPortable instance: " + testTarget, o instanceof NonPortable);
      }
    }
  }

  private void initTest() throws Exception {
    synchronized (root) {
      for (Class k : setClasses) {
        root.add((Set) k.newInstance());
      }
    }
  }

  private Set initSet(Set set) {
    synchronized (set) {
      set.clear();
      set.add("v1");
      set.add("v2");
      set.add("v3");
      return set;
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = SetLocalStateTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");
    config.addIncludePattern(GenericLocalStateTestApp.class.getName() + "$*");
    config.addExcludePattern(NonPortable.class.getName());

    config.addWriteAutolock("* " + testClass + "*.initSet(..)");
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
      Set s = (Set) o;
      s.add("v4");
    }
  }

  static class AddAllMutator implements Mutator {
    public void doMutate(Object o) {
      Set s = (Set) o;
      Set anotherSet = new HashSet();
      anotherSet.add("v");
      s.addAll(anotherSet);
    }
  }

  static class AddAllNonPortableMutator implements Mutator {
    public void doMutate(Object o) {
      Set s = (Set) o;
      Set anotherSet = new HashSet();
      anotherSet.add("v4");
      anotherSet.add("v5");
      anotherSet.add("v6");
      anotherSet.add(new NonPortable());
      anotherSet.add("v7");
      s.addAll(anotherSet);
    }
  }

  static class RemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Set s = (Set) o;
      s.remove("v1");
    }
  }

  static class ClearMutator implements Mutator {
    public void doMutate(Object o) {
      Set s = (Set) o;
      s.clear();
    }
  }

  static class RemoveAllMutator implements Mutator {
    public void doMutate(Object o) {
      Set s = (Set) o;
      Set a = new HashSet();
      a.add("v1");
      a.add("v2");
      s.removeAll(a);
    }
  }

  static class RetainAllMutator implements Mutator {
    public void doMutate(Object o) {
      Set s = (Set) o;
      Set a = new HashSet();
      a.add("v1");
      a.add("v2");
      s.retainAll(a);
    }
  }

  static class IteratorRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Set s = (Set) o;
      for (Iterator it = s.iterator(); it.hasNext();) {
        it.next();
        it.remove();
      }
    }
  }
}
