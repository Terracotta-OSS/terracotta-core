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

import java.util.HashMap;
import java.util.Map;

/**
 * Ensure that we can use instances of java.lang.Object in DSO managed graphs
 */
public class ShareableJavaLangObjectTestApp extends AbstractTransparentApp {

  private final Map    root       = new HashMap();
  private final Object objectRoot = new Object();

  public ShareableJavaLangObjectTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

    if (getParticipantCount() < 2) { throw new RuntimeException("must have at least two participants"); }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ShareableJavaLangObjectTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "root");
    spec.addRoot("objectRoot", "objectRoot");

    config.addIncludePattern(ObjectHolder.class.getName());
  }

  public void run() {
    Object o = objectRoot;
    Assert.assertNotNull(o);

    final Object object;

    synchronized (root) {
      if (root.size() == 0) {
        root.put("object", new Object());
      }
      object = root.get("object");
    }

    // synchronizing on the Object instance will create a transaction in which we can write to root in
    synchronized (object) {
      root.put(getApplicationId(), null);
    }

    // Also make sure a physical object with an Object reference works okay
    synchronized (root) {
      root.put("objectholder", new ObjectHolder());
    }
  }

  private static class ObjectHolder {
    @SuppressWarnings("unused")
    final Object heldObject = new Object();
  }
}
