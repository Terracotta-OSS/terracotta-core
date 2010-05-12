/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.dump.DumpServer;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CyclicBarrier;

public class L2DumperTestApp extends AbstractTransparentApp {

  private ArrayList<IntNumber> mySharedArrayList;
  private int                  jmxPort;
  private CyclicBarrier        barrier;

  public L2DumperTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    mySharedArrayList = new ArrayList<IntNumber>();
    jmxPort = Integer.parseInt(cfg.getAttribute(ApplicationConfig.JMXPORT_KEY));
  }

  public void run() {
    verify(0);
    int index = waitOnBarrier();

    int totalAdditions = 3000;

    if (index == 0) {
      addToList(totalAdditions);
    }

    waitOnBarrier();

    verify(totalAdditions);

    if (index == 0) {
      takeServerDump();
    }

    waitOnBarrier();
  }

  private void takeServerDump() {
    try {
      long time = System.currentTimeMillis();
      new DumpServer("localhost", jmxPort).dumpServer();
      System.out.println("Time taken for dump = " + (System.currentTimeMillis() - time));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void addToList(int totalAdditions) {
    for (int i = 0; i < 3000; i++) {
      synchronized (mySharedArrayList) {
        IntNumber intNumber = new IntNumber(mySharedArrayList.size());
        mySharedArrayList.add(intNumber);
      }
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

  private void verify(int no) {
    synchronized (mySharedArrayList) {
      Iterator<IntNumber> iter = mySharedArrayList.iterator();
      int temp = -1;
      int counter = 0;

      Assert.assertEquals(mySharedArrayList.size(), no);

      while (iter.hasNext()) {
        temp = iter.next().get();
        Assert.assertEquals(counter, temp);
        counter++;
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = L2DumperTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(L2DumperTestApp.class.getName());
    config.addIncludePattern(L2DumperTestApp.IntNumber.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";

    spec.addRoot("barrier", "barrier");
    config.addWriteAutolock(methodExpression);
    spec.addRoot("mySharedArrayList", "mySharedArrayList");
  }

  class IntNumber {
    private int i;

    public IntNumber(int i) {
      this.i = i;
    }

    public int get() {
      return i;
    }

    public void set(int i) {
      this.i = i;
    }
  }
}