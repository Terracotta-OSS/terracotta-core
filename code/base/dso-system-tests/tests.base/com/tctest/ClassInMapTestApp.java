/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
 * This test makes sure that an instance of java/lang/Class can be used as a key in a map.
 */
public class ClassInMapTestApp extends AbstractTransparentApp {

  private final Map map = new HashMap();

  public ClassInMapTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    if (getParticipantCount() != 2) { throw new IllegalArgumentException("invalid number of participants"); }
  }

  public void run() {
    try {
      run0();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void run0() throws Exception {
    synchronized (map) {
      if (map.isEmpty()) {
        map.put(this.getClass(), "value");
        map.put("key", Object.class);

        // make sure we're actually using DSO ;-)
        while (!map.isEmpty()) {
          map.wait(120000);
        }
      } else {
        Assert.assertEquals("value", map.get(this.getClass()));
        Assert.assertEquals(Object.class, map.get("key"));
        map.remove(this.getClass());
        map.remove("key");
        map.notifyAll();
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ClassInMapTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("map", "map");
  }

}
