/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.Root;
import com.tc.object.config.StandardDSOSpringConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.bean.Singleton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Spring singleton tests
 */
public class Singleton_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 2;

  public Singleton_Test() {
    disableAllUntil("2008-01-01"); // covered by system test
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATIONS);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return SingletonApp.class;
  }

  public static class SingletonApp extends AbstractTransparentApp {
    private List sharedSingletons = new ArrayList();

    public SingletonApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
      testSimpleSingleton();
      testSingletonWithParent();
      testLifeCycle();
    }

    private void testSimpleSingleton() {
      try {
        moveToStageAndWait(10);

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("com/tctest/spring/beanfactory.xml");

        ISingleton singleton = (ISingleton) ctx.getBean("singleton");
        singleton.incrementCounter();

        String applicationId = getApplicationId();
        singleton.setTransientValue(applicationId);

        synchronized (sharedSingletons) {
          sharedSingletons.add(singleton);
        }

        moveToStageAndWait(11);

        synchronized (sharedSingletons) {
          assertTrue("Expected more then one object in the collection", sharedSingletons.size() > 1);

          HashSet transientValues = new HashSet();
          for (Iterator it = sharedSingletons.iterator(); it.hasNext();) {
            ISingleton o = (ISingleton) it.next();
            assertTrue("Found non-singleton object", o == singleton);
            assertTrue("Invalid value in shared field", o.getCounter() > 1);
            transientValues.add(o.getTransientValue());
          }

          // TODO investigate why all transient values are the same in sharedSingletons collection on given node
          // assertEquals("Invalid value in transient field", sharedSingletons.size(), transientValues.size());
        }

        moveToStageAndWait(12);

      } catch (Throwable e) {
        moveToStage(11);
        moveToStage(12);
        notifyError(e);

      } finally {
        clear();

      }
    }

    private void testSingletonWithParent() {
      try {
        moveToStageAndWait(20);

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
                                                                                "com/tctest/spring/beanfactory-withParent.xml");

        ISingleton singleton = (ISingleton) ctx.getBean("singletonDelegator");
        singleton.incrementCounter();

        String applicationId = getApplicationId();
        singleton.setTransientValue(applicationId);

        synchronized (sharedSingletons) {
          sharedSingletons.add(singleton);
        }

        moveToStageAndWait(21);

        synchronized (sharedSingletons) {
          assertTrue("Expected more then one object in the collection", sharedSingletons.size() > 1);

          HashSet transientValues = new HashSet();
          for (Iterator it = sharedSingletons.iterator(); it.hasNext();) {
            ISingleton o = (ISingleton) it.next();
            assertTrue("Found non-singleton object", o == singleton);
            assertTrue("Invalid value in shared field", o.getCounter() > 1);
            transientValues.add(o.getTransientValue());
          }
        }

        moveToStageAndWait(22);

      } catch (Throwable e) {
        moveToStage(21);
        moveToStage(22);
        notifyError(e);

      } finally {
        clear();

      }
    }

    private void testLifeCycle() {
      try {
        moveToStageAndWait(31);

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("com/tctest/spring/beanfactory.xml");

        Singleton singleton = (Singleton) ctx.getBean("singleton");

        List recorder = singleton.getRecorder();
        List localRecorder = singleton.getLocalRecorder();
        List transientRecorder = singleton.getTransientRecorder();

        moveToStageAndWait(32);

        ctx.close();

        moveToStageAndWait(33);

        // System.err.println("### "+Thread.currentThread().getName()+" 1 "+recorder);
        // System.err.println("### "+Thread.currentThread().getName()+" 2 "+localRecorder);
        // System.err.println("### "+Thread.currentThread().getName()+" 3 "+transientRecorder);

        assertEquals("Invalid clustered " + recorder.toString(), 4, recorder.size());
        assertEquals("Invalid local " + localRecorder.toString(), 4, localRecorder.size());
        assertEquals("Invalid transient " + transientRecorder.toString(), 2, transientRecorder.size());

      } catch (Throwable e) {
        moveToStage(31);
        moveToStage(32);
        moveToStage(33);
        notifyError(e);

      } finally {
        clear();

      }
    }

    private void clear() {
      synchronized (sharedSingletons) {
        sharedSingletons.clear();
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.addRoot(new Root("com.tctest.spring.Singleton_Test$SingletonApp", "sharedSingletons", "sharedSingletons"),
                     false);
      config.addAutolock("* com.tctest.spring.Singleton_Test$SingletonApp.*()", ConfigLockLevel.WRITE);

      {
        DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
        springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing
                                                                                      // framework
        springConfig.addConfigPattern("*/beanfactory.xml");
        springConfig.addBean("singleton");
        springConfig.addBean("recorder");
        springConfig.excludeField("singleton", "transientValue");
        springConfig.excludeField("singleton", "transientBoolean");

        config.addDSOSpringConfig(springConfig);
      }

      {
        DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
        springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing
                                                                                      // framework
        springConfig.addConfigPattern("*/beanfactory-withParent.xml");
        springConfig.addBean("singletonDelegator");
        springConfig.excludeField("singletonDelegator", "transientValue");
        springConfig.excludeField("singletonDelegator", "transientBoolean");

        config.addDSOSpringConfig(springConfig);
      }
    }

  }

}
