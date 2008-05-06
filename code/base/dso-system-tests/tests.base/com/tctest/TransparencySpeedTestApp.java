/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.exception.TCRuntimeException;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;

/**
 * @author steve
 */
public class TransparencySpeedTestApp extends AbstractTransparentApp {
  public final static int MUTATOR_COUNT  = 3;
  public final static int ADD_COUNT      = 10;                    // must be divisible by 2
  public final static int VERIFIER_COUNT = 3;

  private static Map      myRoot         = new HashMap();
  private long            count;
  private int             commits        = 0;
  private SynchronizedInt gcount         = new SynchronizedInt(0);

  public TransparencySpeedTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.getOrCreateSpec("com.tctest.TransparencySpeedTestApp$TestObj");
    TransparencyClassSpec spec = config.getOrCreateSpec("com.tctest.TransparencySpeedTestApp");
    spec.addRoot("myRoot", "rootBabyRoot");
    spec.addRoot("gcount", "globalCount");

    String methodExpression = "long com.tctest.TransparencySpeedTestApp.test4(int, java.lang.Object)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "long com.tctest.TransparencySpeedTestApp.test5(int, java.lang.Object)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "void com.tctest.TransparencySpeedTestApp.notifyDone()";
    config.addWriteAutolock(methodExpression);

    spec = config.getOrCreateSpec("com.tctest.TransparencySpeedTestVerifier");

    spec.addRoot("resultRoot", "rootBabyRoot");

    methodExpression = "boolean com.tctest.TransparencySpeedTestVerifier.verify()";

    config.addWriteAutolock(methodExpression);
    new SynchronizedIntSpec().visit(visitor, config);
  }

  public void run() {

    int myId = gcount.increment();
    if (myId > MUTATOR_COUNT) {
      verify();
    } else {
      mutate(myId);
    }
  }

  private void verify() {
    try {
      new TransparencySpeedTestVerifier().verify();
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  public void mutate(int myId) {
    this.count = (myId - 1) * ADD_COUNT;
    System.err.println("AppId is :" + getApplicationId() + " and count  = " + count);
    boolean remove = false;
    long start = System.currentTimeMillis();
    long totalInTXTime = 0;
    for (int i = 0; i < ADD_COUNT; i++) {
      totalInTXTime += test4(i, new Object());
      if (false) totalInTXTime += test5(i, new Object());
      remove = !remove;
    }
    long seconds = (System.currentTimeMillis() - start) / 1000;
    String commitsPerSecond = "DIVIDE BY ZERO!";
    if (seconds != 0) {
      commitsPerSecond = "" + (commits / seconds);
    }
    System.out.println("****Commits:" + commits + " seconds:" + seconds + " commits/second: " + commitsPerSecond
                       + " Total in tx:" + totalInTXTime);
    notifyDone();
  }

  private void notifyDone() {
    synchronized (myRoot) {
      // The guy waiting is in TransparencySpeedTestVerifier ...
      myRoot.notifyAll();
    }
  }

  public long test4(int i, Object foo) {
    synchronized (myRoot) {
      long start = System.currentTimeMillis();
      commits++;
      int s = myRoot.size();
      long c = count++;
      if (myRoot.containsKey(new Long(c))) {
        Assert.eval(false);
      }
      myRoot.put(new Long(c), new TestObj(new TestObj(null)));
      if (myRoot.size() != s + 1) System.out.println("Wrong size!:" + s + " new size:" + myRoot.size());
      Assert.eval(myRoot.size() == s + 1);
      // System.out.println("^^^TOTAL SIZE ADD:" + myRoot.size() + "^^^:" + this);
      return System.currentTimeMillis() - start;
    }
  }

  public long test5(int i, Object foo) {
    synchronized (myRoot) {
      long start = System.currentTimeMillis();
      commits++;
      int s = myRoot.size();
      myRoot.remove(new Long(count - 1));
      if (myRoot.size() != s - 1) System.out.println("Wrong size!:" + s + " new size:" + myRoot.size());
      Assert.eval(myRoot.size() == s - 1);
      // System.out.println("^^^TOTAL SIZE REMOVE:" + myRoot.size() + "^^^:" + this);
      return System.currentTimeMillis() - start;
    }
  }

  public static class TestObj {
    private TestObj obj;
    private String  string  = "Steve";
    private int     integer = 22;
    private boolean bool    = false;
    private Map     map     = new HashMap();

    private TestObj() {
      //
    }

    public TestObj(TestObj obj) {
      this.obj = obj;
      for (int i = 0; i < 30; i++) {
        map.put(new Long(i), new TestObj());
      }
    }

    public Object getObject() {
      return this.obj;
    }

    public boolean check() {
      return string.equals("Steve") && integer == 22 && bool == false;
    }
  }
}