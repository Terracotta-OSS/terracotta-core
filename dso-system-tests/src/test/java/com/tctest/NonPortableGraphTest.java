/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCNonPortableObjectError;
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

  public NonPortableGraphTest() {
    //
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return NonPortableGraphTestApp.class;
  }

  public static class NonPortableGraphTestApp extends AbstractErrorCatchingTransparentApp {
    private final Map         map = new HashMap();
    private Portable          portable;
    private NonPortable       non_portable;
    private final LogAppender dsoLogs;

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

    @Override
    protected void runTest() throws Throwable {
      testNonportableObjectGraph();
    }

    public void testNonportableObjectGraph() {
      try {
        synchronized (map) {
          map.put("mmkay", new NonPortable());
        }
        throw new AssertionError("1. Expecting TCNonPortableObjectError");
      } catch (TCNonPortableObjectError e) {
        // expected
      }

      assertTrue(dsoLogs.getCurrentLogEvent().replaceAll("\\s", "").indexOf(log1.replaceAll("\\s", "")) > 0);

      try {
        portable = new Portable(new NonPortable());
        throw new AssertionError("2. Expecting TCNonPortableObjectError");
      } catch (TCNonPortableObjectError e) {
        // expected
      }

      assertTrue(dsoLogs.getCurrentLogEvent().replaceAll("\\s", "").indexOf(log2.replaceAll("\\s", "")) > 0);

      try {
        non_portable = new NonPortable();
        throw new AssertionError("3. Expecting TCNonPortableObjectError");
      } catch (TCNonPortableObjectError e) {
        // expected
      }

      assertTrue(dsoLogs.getCurrentLogEvent().replaceAll("\\s", "").indexOf(log1.replaceAll("\\s", "")) > 0);
    }

    Portable getPortable() {
      return portable;
    }

    NonPortable getNon_portable() {
      return non_portable;
    }

    private static class NonPortable {
      Map map = new HashMap();

      public NonPortable() {
        map.put("self", this);
      }
    }

    private static class Portable {
      @SuppressWarnings("unused")
      Object obj;

      public Portable(Object obj) {
        this.obj = obj;
      }
    }

    private static class LogAppender implements TCAppender {
      StringBuffer buffer = new StringBuffer();

      public void append(LogLevel arg0, Object arg1, Throwable arg2) {
        buffer.append(arg1 + "\n");
      }

      public String getCurrentLogEvent() {
        String s = buffer.toString();
        buffer = new StringBuffer();
        return s;
      }

    }

    private static final String log1 = "!! com.tctest.NonPortableGraphTest$NonPortableGraphTestApp$NonPortable (id 0)"
                                       + "     Map map = (HashMap, id 1)" + "       [entry 0]"
                                       + "         key = \"self\"" + "!!       value = (ref id 0)";

    private static final String log2 = "   com.tctest.NonPortableGraphTest$NonPortableGraphTestApp$Portable (id 0)"
                                       + "!!   Object obj = (com.tctest.NonPortableGraphTest$NonPortableGraphTestApp$NonPortable, id 1)"
                                       + "       Map map = (HashMap, id 2)" + "         [entry 0]"
                                       + "           key = \"self\"" + "!!         value = (ref id 1)";
  }
}
