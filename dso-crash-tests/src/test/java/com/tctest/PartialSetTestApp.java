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
import com.tctest.runner.AbstractTransparentApp;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CyclicBarrier;

public class PartialSetTestApp extends AbstractTransparentApp {
  private final CyclicBarrier barrier;
  private final Set           myTreeSetRoot;
  private final int           NUMBERS_ADDED_IN_ONE_ITERATION = 10000;
  private final int           NO_OF_ITERATIONS               = 5;

  public PartialSetTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    myTreeSetRoot = new TreeSet();
  }

  private int waitOnBarrier() {
    try {
      return barrier.await();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void addToSet(int totalNumbers) {
    for (int i = 0; i < totalNumbers; i++) {
      synchronized (myTreeSetRoot) {
        int number = myTreeSetRoot.size();
        myTreeSetRoot.add(new PartialSetNode(number));
      }
    }
  }

  private void verify(int totalNumbers) {
    synchronized (myTreeSetRoot) {
      int counter = 0;
      for (Iterator iter = myTreeSetRoot.iterator(); iter.hasNext();) {
        PartialSetNode node = (PartialSetNode) iter.next();
        Assert.assertEquals(counter, node.getNumber());
        counter++;
      }

      Assert.assertEquals(totalNumbers, counter);
    }
  }

  public void run() {
    for (int i = 1; i <= NO_OF_ITERATIONS; i++) {
      int index = waitOnBarrier();
      if (index != 0) {
        addToSet(NUMBERS_ADDED_IN_ONE_ITERATION);
      }
      waitOnBarrier();
      verify(i * NUMBERS_ADDED_IN_ONE_ITERATION);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = PartialSetTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("myTreeSetRoot", "myTreeSetRoot");
    config.getOrCreateSpec(PartialSetNode.class.getName());
  }
}
