/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

import com.tc.object.bytecode.TransparentAccess;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractTransparentApp;

public class InstrumentNothingTest extends TransparentTestBase implements TestConfigurator {

  protected Class getApplicationClass() {
    return InstrumentNothingTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }

  public static final class InstrumentNothingTestApp extends AbstractTransparentApp {
  
    public InstrumentNothingTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }
    
    public void run() {
      Class[] interfaces = getClass().getInterfaces();
      for (int i=0; i<interfaces.length; i++) {
        if (TransparentAccess.class.getName().equals(interfaces[i].getName())) {
          throw new AssertionError("I shouldn't have been instrumented, but I was!");
        }
      }
    }
  }
  
}
