/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.logging.CustomerLogging;
import com.tc.logging.LogLevel;
import com.tc.logging.TCAppender;
import com.tc.logging.TCLogging;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Map;

public class NonPortableGraphTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return NonPortableGraphTestApp.class;
  }

  public static class NonPortableGraphTestApp extends AbstractErrorCatchingTransparentApp {
    private Map         map = new HashMap();
    private Portable    portable;
    private NonPortable non_portable;
    private LogAppender dsoLogs;

    public NonPortableGraphTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      dsoLogs = new LogAppender();

      TCLogging.addAppender(CustomerLogging.getDSORuntimeLogger().getName(), dsoLogs);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

      String testClass = NonPortableGraphTestApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      config.addIncludePattern(testClass);
      config.addIncludePattern(testClass + "$Portable");

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("map", "map");
      spec.addRoot("portable", "portable");
      spec.addRoot("non_portable", "non_portable");
    }

    protected void runTest() throws Throwable {
      testNonportableObjectGraph();
    }

    public void testNonportableObjectGraph() {
      try {
        synchronized (map) {
          map.put("mmkay", new NonPortable());
        }
        throw new Exception("1. Expecting TCNonPortableObjectError");
      } catch (Throwable e) {
        if (!e.getClass().getName().equals("com.tc.exception.TCNonPortableObjectError")) throw new RuntimeException(e);
      }

      assertTrue(dsoLogs.getCurrentLogEvent().replaceAll("\\s", "").indexOf(log1.replaceAll("\\s", "")) > 0);

      try {
        portable = new Portable(new NonPortable());
        throw new Exception("2. Expecting TCNonPortableObjectError");
      } catch (Throwable e) {
        if (!e.getClass().getName().equals("com.tc.exception.TCNonPortableObjectError")) throw new RuntimeException(e);
      }

      assertTrue(dsoLogs.getCurrentLogEvent().replaceAll("\\s", "").indexOf(log2.replaceAll("\\s", "")) > 0);

      try {
        non_portable = new NonPortable();
        throw new Exception("3. Expecting TCNonPortableObjectError");
      } catch (Throwable e) {
        if (!e.getClass().getName().equals("com.tc.exception.TCNonPortableObjectError")) throw new RuntimeException(e);
      }

      assertTrue(dsoLogs.getCurrentLogEvent().replaceAll("\\s", "").indexOf(log1.replaceAll("\\s", "")) > 0);
    }

    private static class NonPortable {
      Map map = new HashMap();

      public NonPortable() {
        map.put("self", this);
      }
    }

    private static class Portable {
      Object obj;

      public Portable(Object obj) {
        this.obj = obj;
      }
    }

    private static class LogAppender implements TCAppender {
      String currentLogEvent;

      public void append(LogLevel arg0, Object arg1, Throwable arg2) {
        currentLogEvent = (String) arg1;
      }

      public String getCurrentLogEvent() {
        return currentLogEvent;
      }

    }

    private static final String log1 = "!! com.tctest.NonPortableGraphTest$NonPortableGraphTestApp$NonPortable (id 0)"
                                       + "     Map map = (HashMap, id 1)"
                                       + "       [entry 0]"
                                       + "         key = \"self\""
                                       + "!!       value = (ref id 0)";

    private static final String log2 = "   com.tctest.NonPortableGraphTest$NonPortableGraphTestApp$Portable (id 0)"
                                       + "!!   Object obj = (com.tctest.NonPortableGraphTest$NonPortableGraphTestApp$NonPortable, id 2)"
                                       + "       Map map = (HashMap, id 3)"
                                       + "         [entry 0]"
                                       + "           key = \"self\""
                                       + "!!         value = (ref id 2)";
  }
}
