/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class TreeSetTestApp extends AbstractTransparentApp {

  // plain old TreeSet
  private final TreeSet            set              = new TreeSet();

  // Use a comparator with a shared TreeSet too. If the comparator doesn't make it across VMs,
  // we should get some ClassCastExceptions.
  private final TreeSet            set2             = new TreeSet(new WrappedStringComparator());

  private final CyclicBarrier      barrier;

  private final SubSetSharedObject subSetSharedRoot = new SubSetSharedObject(0);

  public TreeSetTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      subSetTesting();
      headSetTesting();
      tailSetTesting();
      viewSetSharedTesting();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private int getKey() throws Exception {
    int key = -1;
    synchronized (subSetSharedRoot) {
      if (subSetSharedRoot.getKey() != 0) {
        subSetSharedRoot.setKey(0);
      }
    }
    barrier.barrier();

    synchronized (subSetSharedRoot) {
      key = subSetSharedRoot.getKey();
      if (key == 0) {
        subSetSharedRoot.setKey(1);
      }
    }
    return key;
  }

  private void subSetTesting() throws Exception {
    clear();
    initializeSets();

    // subSet() testing.

    int key = getKey();
    barrier.barrier();

    if (key == 0) {
      synchronized (set) {
        Object fromKey = new Integer(0);
        Object toKey = new Integer(10);
        Set subSet = set.subSet(fromKey, toKey);
        subSet.add(new Integer(1));
      }
    }
    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set.size());
    Assert.assertTrue(set.contains(new Integer(1)));

    barrier.barrier();
    key = getKey();
    barrier.barrier();

    if (key == 0) {
      synchronized (set2) {
        Object fromKey = new WrappedString(0);
        Object toKey = new WrappedString(10);
        Set subSet = set2.subSet(fromKey, toKey);
        subSet.add(new WrappedString(1));
      }
    }
    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set2.size());
    Assert.assertTrue(set2.contains(new WrappedString(1)));
    barrier.barrier();
  }

  private void headSetTesting() throws Exception {
    clear();
    initializeSets();

    int key = getKey();
    barrier.barrier();

    // headSet() testing.
    if (key == 0) {
      synchronized (set) {
        Object toKey = new Integer(2);
        Set headSet = set.headSet(toKey);
        headSet.add(new Integer(1));
      }
    }
    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set.size());
    Assert.assertTrue(set.contains(new Integer(1)));

    barrier.barrier();

    key = getKey();
    barrier.barrier();

    if (key == 0) {
      synchronized (set2) {
        Object toKey = new WrappedString(2);
        Set headSet = set2.headSet(toKey);
        headSet.add(new WrappedString(1));
      }
    }

    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set2.size());
    Assert.assertTrue(set2.contains(new WrappedString(1)));

    barrier.barrier();
  }

  private void tailSetTesting() throws Exception {
    clear();
    initializeSets();

    // tailSet() testing.
    int key = getKey();
    barrier.barrier();

    if (key == 0) {
      synchronized (set) {
        Object fromKey = new Integer(0);
        Set tailSet = set.tailSet(fromKey);
        tailSet.add(new Integer(1));
      }
    }

    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set.size());
    Assert.assertTrue(set.contains(new Integer(1)));

    barrier.barrier();

    key = getKey();
    barrier.barrier();

    if (key == 0) {
      synchronized (set2) {
        Object fromKey = new WrappedString(0);
        Set tailSet = set2.tailSet(fromKey);
        tailSet.add(new WrappedString(1));
      }
    }

    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set2.size());
    Assert.assertTrue(set2.contains(new WrappedString(1)));
    barrier.barrier();

    clear();
    initializeSets();

    key = getKey();
    barrier.barrier();

    // tailSet() clear testing.
    if (key == 0) {
      synchronized (set) {
        Object fromKey = new Integer(0);
        Set tailSet = set.tailSet(fromKey);
        tailSet.clear();
      }
    }
    barrier.barrier();

    Assert.assertEquals(0, set.size());

    barrier.barrier();

    key = getKey();
    barrier.barrier();

    if (key == 0) {
      synchronized (set2) {
        Object fromKey = new WrappedString(0);
        Set tailSet = set2.tailSet(fromKey);
        tailSet.clear();
      }
    }
    barrier.barrier();

    Assert.assertEquals(0, set2.size());
    barrier.barrier();
  }

  private void viewSetSharedTesting() throws Exception {
    clear();
    initializeSets();

    int key = getKey();
    barrier.barrier();

    // subSet() share testing.
    if (key == 0) {
      Object fromKey = new Integer(0);
      Object toKey = new Integer(10);
      Set subSet = set.subSet(fromKey, toKey);
      synchronized (subSetSharedRoot) {
        subSetSharedRoot.setSet(subSet);
      }
      synchronized (subSetSharedRoot.getSet()) {
        subSetSharedRoot.getSet().add(new Integer(1));
      }
    }

    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set.size());
    Assert.assertTrue(set.contains(new Integer(1)));

    barrier.barrier();

    key = getKey();
    barrier.barrier();

    if (key == 0) {
      Object fromKey = new WrappedString(0);
      Object toKey = new WrappedString(10);
      Set subSet = set2.subSet(fromKey, toKey);
      synchronized (subSetSharedRoot) {
        subSetSharedRoot.setSet(subSet);
      }
      synchronized (subSetSharedRoot.getSet()) {
        subSetSharedRoot.getSet().add(new WrappedString(1));
      }
    }

    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set2.size());
    Assert.assertTrue(set2.contains(new WrappedString(1)));

    barrier.barrier();

    clear();
    initializeSets();

    key = getKey();
    barrier.barrier();

    // headSet() share testing.
    if (key == 0) {
      Object toKey = new Integer(10);
      Set headSet = set.headSet(toKey);
      synchronized (subSetSharedRoot) {
        subSetSharedRoot.setSet(headSet);
      }
      synchronized (subSetSharedRoot.getSet()) {
        subSetSharedRoot.getSet().add(new Integer(1));
      }
    }

    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set.size());
    Assert.assertTrue(set.contains(new Integer(1)));

    barrier.barrier();

    key = getKey();
    barrier.barrier();

    if (key == 0) {
      Object toKey = new WrappedString(10);
      Set headSet = set2.headSet(toKey);
      synchronized (subSetSharedRoot) {
        subSetSharedRoot.setSet(headSet);
      }
      synchronized (subSetSharedRoot.getSet()) {
        subSetSharedRoot.getSet().add(new WrappedString(1));
      }
    }

    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set2.size());
    Assert.assertTrue(set2.contains(new WrappedString(1)));

    barrier.barrier();

    clear();
    initializeSets();

    key = getKey();
    barrier.barrier();

    // tailSet() share testing.
    if (key == 0) {
      Object fromKey = new Integer(0);
      Set tailSet = set.tailSet(fromKey);
      synchronized (subSetSharedRoot) {
        subSetSharedRoot.setSet(tailSet);
      }

      synchronized (subSetSharedRoot.getSet()) {
        subSetSharedRoot.getSet().add(new Integer(1));
      }
    }

    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set.size());
    Assert.assertTrue(set.contains(new Integer(1)));

    barrier.barrier();

    key = getKey();
    barrier.barrier();

    if (key == 0) {
      Object fromKey = new WrappedString(0);
      Set tailSet = set2.tailSet(fromKey);
      synchronized (subSetSharedRoot) {
        subSetSharedRoot.setSet(tailSet);
      }

      synchronized (subSetSharedRoot.getSet()) {
        subSetSharedRoot.getSet().add(new WrappedString(1));
      }
    }
    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set2.size());
    Assert.assertTrue(set2.contains(new WrappedString(1)));

    barrier.barrier();

    clear();
    initializeSets();

    key = getKey();
    barrier.barrier();

    // subSet().subSet() share testing.
    if (key == 0) {
      Object fromKey = new Integer(0);
      Object toKey = new Integer(10);
      Object toKey2 = new Integer(5);
      SortedSet subSet = set.subSet(fromKey, toKey);
      Set subSet2 = subSet.subSet(fromKey, toKey2);
      synchronized (subSetSharedRoot) {
        subSetSharedRoot.setSet(subSet2);
      }
      synchronized (subSetSharedRoot.getSet()) {
        subSetSharedRoot.getSet().add(new Integer(1));
      }
    }

    barrier.barrier();

    Assert.assertEquals(getParticipantCount() + 1, set.size());
    Assert.assertTrue(set.contains(new Integer(1)));

    barrier.barrier();

    key = getKey();
    barrier.barrier();
    if (key == 0) {
      Object fromKey = new WrappedString(0);
      Object toKey = new WrappedString(10);
      Object toKey2 = new WrappedString(5);
      SortedSet subSet = set2.subSet(fromKey, toKey2);
      Set subSet2 = subSet.subSet(fromKey, toKey);
      synchronized (subSetSharedRoot) {
        subSetSharedRoot.setSet(subSet2);
      }
      synchronized (subSetSharedRoot.getSet()) {
        subSetSharedRoot.getSet().add(new WrappedString(1));
      }
    }

    barrier.barrier();
    Assert.assertEquals(getParticipantCount() + 1, set2.size());
    Assert.assertTrue(set2.contains(new WrappedString(1)));

    barrier.barrier();
  }

  private void clear() throws Exception {
    synchronized (set) {
      set.clear();
    }
    synchronized (set2) {
      set2.clear();
    }

    synchronized (subSetSharedRoot) {
      subSetSharedRoot.clear();
    }

    barrier.barrier();
  }

  private void initializeSets() throws Exception {
    synchronized (subSetSharedRoot) {
      if (subSetSharedRoot.getKey() != 0) {
        subSetSharedRoot.setKey(0);
      }
    }
    barrier.barrier();

    synchronized (set) {
      int key = subSetSharedRoot.getKey();
      set.add(new Integer(key));
      subSetSharedRoot.setKey(key + 2);
    }
    barrier.barrier();
    synchronized (subSetSharedRoot) {
      if (subSetSharedRoot.getKey() != 0) {
        subSetSharedRoot.setKey(0);
      }
    }
    barrier.barrier();
    synchronized (set2) {
      int key = subSetSharedRoot.getKey();
      set2.add(new WrappedString(key));
      subSetSharedRoot.setKey(key + 2);
    }
    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = TreeSetTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("set", "set");
    spec.addRoot("set2", "set2");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("subSetSharedRoot", "subSetSharedRoot");
  }

  // The main purpose of this class is that it does NOT implement Comparable
  private static class WrappedString {
    private final String string;

    WrappedString(int i) {
      this.string = String.valueOf(i);
    }

    String getString() {
      return this.string;
    }
  }

  private static class WrappedStringComparator implements Comparator {

    public int compare(Object o1, Object o2) {
      WrappedString ws1 = (WrappedString) o1;
      WrappedString ws2 = (WrappedString) o2;
      return ws1.getString().compareTo(ws2.getString());
    }

  }

  /**
   * The main purpose of this class is for to generate the key for the subSet(), headSet(), and tailSet() testing.
   */
  private static class SubSetSharedObject {
    private int key;
    private Set set;

    public SubSetSharedObject(int key) {
      this.key = key;
    }

    public int getKey() {
      return key;
    }

    public void setKey(int key) {
      this.key = key;
    }

    public Set getSet() {
      return set;
    }

    public void setSet(Set set) {
      this.set = set;
    }

    public void clear() {
      this.key = 0;
      this.set = null;
    }
  }
}
