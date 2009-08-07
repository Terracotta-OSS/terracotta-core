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

/**
 * Test that makes sure "shadowed" variables work correctly with DSO
 */
public class ShadowVariableTestApp extends AbstractTransparentApp {
  private ShadowSub root;

  public ShadowVariableTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {

    root = new ShadowSub();
    synchronized (root) {
      if (root.getBaseMyNumber() == null) {
        root.setBaseMyNumber(new Integer(1));
        root.setSubMyNumber(new Integer(2));
      }
    }

    Assert.assertNotNull(root.getBaseMyNumber());
    Assert.assertNotNull(root.getSubMyNumber());

    Assert.eval(root.getBaseMyNumber().equals(new Integer(1)));
    Assert.eval(root.getSubMyNumber().equals(new Integer(2)));

    Assert.assertEquals(0, root.getPublicInt());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ShadowVariableTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(ShadowBase.class.getName());
    config.addIncludePattern(ShadowSub.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "shadowTestRoot");
  }

  @SuppressWarnings("unused")
  private static class ShadowBase {
    // NOTE: It is very important that the shadow fields be of the exact same type
    // STOP: These fields are "shadowed" on purpose -- do not rename to fix eclipse warnings
    private Integer     myNumber  = null;
    protected final int finalInt  = 1;
    public int          publicInt = 10;

    public void setBaseMyNumber(Integer value) {
      this.myNumber = value;
    }

    public Integer getBaseMyNumber() {
      return this.myNumber;
    }

  }

  @SuppressWarnings("unused")
  private static class ShadowSub extends ShadowBase {
    // NOTE: It is very important that the shadow fields be of the exact same type
    // STOP: These fields are "shadowed" on purpose -- do not rename to fix eclipse warnings
    private Integer     myNumber = null;
    @SuppressWarnings("hiding")
    protected final int finalInt = 2;
    @SuppressWarnings("hiding")
    public int          publicInt;

    public void setSubMyNumber(Integer value) {
      this.myNumber = value;
    }

    public Integer getSubMyNumber() {
      return this.myNumber;
    }

    public int getPublicInt() {
      return publicInt;
    }

  }

}
