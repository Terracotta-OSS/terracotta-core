/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * @author steve
 */
public class TransparentListApp extends AbstractTransparentApp {
  private final static int ACTION_COUNT = 5;

  private String           putterName;
  private List             queue        = new LinkedList();
  private Random           random       = new Random();

  public TransparentListApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.putterName = "TransparentListApp.putter(" + appId + ")";
  }
  
  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = TransparentListApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
    spec.addRoot("queue", "sharedQueue");
    testClassName = TransparentListApp.Action.class.getName();
    spec = config.getOrCreateSpec(testClassName);

    String methodExpression = "void com.tctest.TransparentListApp.put(com.tctest.TransparentListApp$Action)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "boolean com.tctest.TransparentListApp.execute(java.lang.String)";
    config.addWriteAutolock(methodExpression);

  }

  public void run() {
    for (int i = 0; i < ACTION_COUNT; i++) {
      put(new Action(i, putterName));
      ThreadUtil.reallySleep(random.nextInt(100));
    }
    while (execute(putterName)) {
      //
    }
  }

  private void put(Action action) {
    synchronized (queue) {
      queue.add(action);
    }
  }

  private boolean execute(String executerName) {
    synchronized (queue) {
      if (queue.size() == 0) { return false; }
      Action action = (Action) queue.remove(0);
      action.execute(executerName);
      return true;
    }
  }

  public static class Action {
    private int    count;
    private String putter;

    public Action(int count, String putter) {
      this.count = count;
      this.putter = putter;
    }

    public void execute(String executerName) {
      System.out.println("*** Executing: Count:" + count + " putter:" + putter + " executer:" + executerName);
    }

    public String toString() {
      return "Count:" + count + " putter:" + putter;
    }
  }

}