/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.transparency.MatchingAutolockedSubclass;
import com.tctest.transparency.MatchingSubclass1;
import com.tctest.transparency.MatchingSubclass2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Kuleshov
 */
public class SubtypeMatchingTestApp extends AbstractTransparentApp {

  private List list = new ArrayList();

  public SubtypeMatchingTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    testHonorTransient();
    testAutolock();
  }

  private void testHonorTransient() {
    MatchingSubclass1 c1 = new MatchingSubclass1();
    MatchingSubclass2 c2 = new MatchingSubclass2();
    synchronized (list) {
      list.add(c1);
      list.add(c2);
    }
    
    c1.setBoo1("boo1");
    c1.setBoo("boo1");
    
    c2.setBoo("boo2");

    try {
      c2.setFoo("foo");
      throw new RuntimeException("Should not allow to change MatchingSubclass2.foo without lock");
    } catch (UnlockedSharedObjectException e) {
      // 
    }
    
    try {
      c1.setFoo("foo");
      throw new RuntimeException("Should not allow to change MatchingSubclass1.foo without lock");
    } catch (UnlockedSharedObjectException e) {
      // 
    }

    try {
      c1.setFoo1("foo1");
      throw new RuntimeException("Should not allow to change MatchingClass.foo1 without lock");
    } catch (UnlockedSharedObjectException e) {
      // 
    }
  }

  private void testAutolock() {
    MatchingAutolockedSubclass c1 = new MatchingAutolockedSubclass();
    synchronized (list) {
      list.add(c1);
    }
    c1.setMoo("moo1");

    MatchingAutolockedSubclass c2 = new MatchingAutolockedSubclass(list);
    c2.setMoo("moo2");
  }
  
  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = SubtypeMatchingTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("list", "list");
    
    config.addWriteAutolock("* " + testClass + "*.*(..)");

    config.addIncludePattern("com.tctest.transparency.MarkerInterface+", true);
    config.addIncludePattern("com.tctest.transparency.MatchingClass+", true);

    config.addAutolock("* com.tctest.transparency.MatchingClass+.set*(..)", ConfigLockLevel.WRITE);
    config.addAutolock("* com.tctest.transparency.MatchingClass+.__INIT__(..)", ConfigLockLevel.WRITE);
    // config.addAutolock("* " + MatchingAutolockedSubclass.class.getName() + ".*(..)", ConfigLockLevel.WRITE);
  }
  
}

