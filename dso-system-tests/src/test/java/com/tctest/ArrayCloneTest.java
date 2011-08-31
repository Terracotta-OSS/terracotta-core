/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public class ArrayCloneTest extends TransparentTestBase {

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(5);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final Object[] root = new Object[] { new Object(), new Object(), new Object(), new Object(), new Object(),
                                    new Object(), new Object(), new Object(), new Object(), new Object(), new Object(),
                                    new Object(), new Object(), new Object(), new Object() };

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);

    }

    @Override
    protected void runTest() throws Throwable {
      Object[] rootRef = root;

      // make sure the root is shared -- if not the test will pass but won't really be testing anything :-)
      Assert.assertNotNull(ManagerUtil.getObject(rootRef));

      // NOTE: depending on which javac you use, this clone() call might look like it is being made to java/lang/Object
      // or perhaps [Ljava/lang/Object; (CDV-1234)
      Object[] clone = rootRef.clone();
      for (Object o : clone) {
        if (o == null) { throw new AssertionError(); }
      }

    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("root", "root");
    }
  }

}
