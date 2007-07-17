/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.Root;
import com.tc.object.config.StandardDSOSpringConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.runtime.Vm;
import com.tctest.TransparentTestBase;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.spring.bean.Recorder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Test for <code>InitializingBean</code>.
 *
 * @see org.springframework.beans.factory.InitializingBean
 */
public class InitializingBean_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 2;

  public InitializingBean_Test() {
    if (Vm.isIBM()) {
      this.disableAllUntil("2007-10-01");
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATIONS);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return InitializingBeanApp.class;
  }

  public static class InitializingBeanApp extends AbstractTransparentApp {
    private List sharedSingletons = new ArrayList();

    public InitializingBeanApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
      try {
        ClassPathXmlApplicationContext ctx;
        Recorder recorder;

        synchronized (sharedSingletons) {
          ctx = new ClassPathXmlApplicationContext("com/tctest/spring/beanfactory-init.xml");
          recorder = (Recorder) ctx.getBean("recorder");
        }

        moveToStageAndWait(1);

        int localInitBeanValues = 0;
        int distributedInitBeanValues = 0;
        for (Iterator it = recorder.getValues().iterator(); it.hasNext();) {
          String value = (String) it.next();
          if ("localInitBean".equals(value)) {
            localInitBeanValues++;
          } else if ("distributedInitBean".equals(value)) {
            distributedInitBeanValues++;
          }
        }

        assertEquals(NODE_COUNT, localInitBeanValues);
        assertEquals(NODE_COUNT, distributedInitBeanValues);

      } catch (Throwable e) {
        moveToStage(1);
        notifyError(e);

      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.addRoot(new Root("com.tctest.spring.MultipleContexts_Test$InitializingBeanApp", "sharedSingletons",
                              "sharedSingletons"), false);

      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing
                                                                                    // framework
      springConfig.addConfigPattern("*/beanfactory-init.xml");
      springConfig.addBean("recorder");
      springConfig.addBean("distributedInitBean");
      config.addDSOSpringConfig(springConfig);
    }

  }

}
