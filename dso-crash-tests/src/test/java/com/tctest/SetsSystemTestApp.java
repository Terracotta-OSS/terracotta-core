/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CyclicBarrier;

public class SetsSystemTestApp extends AbstractTransparentApp {
  private final ServerControl                 serverControl;
  private final CyclicBarrier                 barrier;
  private final TreeSet<PartialSetNode>       myTreeSet;
  private final LinkedHashSet<PartialSetNode> myLinkedHashSet;
  private final HashSet<PartialSetNode>       myHashSet;
  private final static int              NUMBERS_ADDED = 5000;

  public SetsSystemTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    serverControl = cfg.getServerControl();
    barrier = new CyclicBarrier(getParticipantCount());
    myTreeSet = new TreeSet();
    myLinkedHashSet = new LinkedHashSet();
    myHashSet = new HashSet();
  }

  public void run() {
    int index = waitOnBarrier();
    if (index != 0) {
      addElementsToSets();
    }

    index = waitOnBarrier();
    if (index != 0) restartServer();
    waitOnBarrier();

    validate();
  }

  private void validate() {
    validateSet(myTreeSet, true);
    validateSet(myLinkedHashSet, true);
    validateSet(myHashSet, false);
  }

  private void validateSet(Set<PartialSetNode> set, boolean verifyOrder) {
    int count = 0;
    synchronized (set) {
      Assert.assertEquals(NUMBERS_ADDED, set.size());

      for (Iterator<PartialSetNode> iter = set.iterator(); iter.hasNext();) {
        PartialSetNode node = iter.next();
        if (verifyOrder) Assert.assertEquals(count, node.getNumber());
        else Assert.assertTrue(set.contains(new PartialSetNode(count)));

        count++;
      }
    }
  }

  private void addElementsToSets() {
    addElementsToSet(myHashSet);
    addElementsToSet(myLinkedHashSet);
    addElementsToSet(myTreeSet);
  }

  private void addElementsToSet(Set<PartialSetNode> set) {
    synchronized (set) {
      for (int i = 0; i < NUMBERS_ADDED; i++) {
        Assert.assertTrue(set.add(new PartialSetNode(i)));
      }
    }
  }

  private void restartServer() {
    try {
      System.out.println("Crashing the Server ...");
      serverControl.crash();
      ThreadUtil.reallySleep(10000);
      System.out.println("Re-starting the Server ...");
      serverControl.start();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private int waitOnBarrier() {
    int index = -1;
    try {
      index = barrier.await();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return index;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = SetsSystemTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(SetsSystemTestApp.class.getName());
    config.addIncludePattern(PartialSetNode.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";

    spec.addRoot("myTreeSet", "myTreeSet");
    spec.addRoot("myLinkedHashSet", "myLinkedHashSet");
    spec.addRoot("myHashSet", "myHashSet");
    spec.addRoot("barrier", "barrier");
    config.addWriteAutolock(methodExpression);
  }
}