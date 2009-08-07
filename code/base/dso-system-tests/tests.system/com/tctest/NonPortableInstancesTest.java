/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.logging.CustomerLogging;
import com.tc.logging.LogLevel;
import com.tc.logging.TCAppender;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NonPortableInstancesTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final Object[]    array          = new Object[1];
    private final Ref         physicalObject = new Ref();
    private final List        logicalObject  = new ArrayList();
    private Object            nonPortableRoot;

    private final Map         map            = new LinkedHashMap();

    private final LogAppender logEvents;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);

      logEvents = new LogAppender();

      TCLogging.addAppender(CustomerLogging.getDSORuntimeLogger().getName(), logEvents);
    }

    public Object getNonPortableRoot() {
      // this method here to silence compiler warning
      return nonPortableRoot;
    }

    @Override
    protected void runTest() throws Throwable {

      // array elements are checked for portability before traversing
      try {
        synchronized (array) {
          array[0] = new NotPortable();
        }
        throw new AssertionError();
      } catch (TCNonPortableObjectError e) {
        // expected
      }
      validate(1);

      // field sets are checked for portability before traversing
      try {
        synchronized (physicalObject) {
          physicalObject.setRef(new NotPortable());
        }
        throw new AssertionError();
      } catch (TCNonPortableObjectError e) {
        // expected
      }
      validate(1);

      // params to methods on logical types are checked for portability before traversing
      try {
        synchronized (logicalObject) {
          logicalObject.add(new NotPortable());
        }
        throw new AssertionError();
      } catch (TCNonPortableObjectError e) {
        // expected
      }
      validate(1);

      // root values are checked for portability before traversing
      try {
        nonPortableRoot = new NotPortable();
        throw new AssertionError();
      } catch (TCNonPortableObjectError e) {
        // expected
      }
      validate(1);

      // This test will pass the initial portability checks (both params to put() are portable), but the value object
      // contains a reference to a non-portable type
      try {
        synchronized (map) {
          map.put("key", new Portable());
        }
        throw new AssertionError();
      } catch (TCNonPortableObjectError e) {
        // expected
      }
      validate(2);

    }

    private void validate(int i) throws IOException {
      String expect = getExpected(i);
      String actual = logEvents.takeLoggedMessages();

      expect = expect.replaceAll("\r", "");
      actual = actual.replaceAll("\r", "");

      Assert.assertEquals(expect, actual);
    }

    private String getExpected(int i) throws IOException {
      String resource = ClassUtils.getShortClassName(getClass()) + "-dump" + i + ".txt";
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      InputStream in = null;
      try {
        in = getClass().getResourceAsStream(resource);
        if (in == null) {
          fail("missing resource: " + resource);
        }
        IOUtils.copy(in, baos);
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      }

      baos.flush();
      return new String(baos.toByteArray());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(Ref.class.getName());
      TransparencyClassSpec spec = config.getOrCreateSpec(Portable.class.getName());
      spec.setHonorTransient(true);
      spec.addTransient("ss");

      String testClass = App.class.getName();
      spec = config.getOrCreateSpec(testClass);
      String methodExpr = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpr);

      spec.addRoot("logicalObject", "logicalObject");
      spec.addRoot("array", "array");
      spec.addRoot("physicalObject", "physicalObject");
      spec.addRoot("nonPortableRoot", "nonPortableRoot");

      spec.addRoot("map", "map");
    }

  }

  @SuppressWarnings("unused")
  private static class Ref {
    private Object ref;

    Object getRef() {
      return ref;
    }

    void setRef(Object ref) {
      this.ref = ref;
    }

  }

  @SuppressWarnings("unused")
  private static class Portable {
    Ref               ref              = new Ref();

    transient Runtime honeredTransient = Runtime.getRuntime();
    ServerSocket      ss;                                     // transient by DSO config

    final Thread      nullThread1      = null;
    final Thread      nullThread2      = null;

    Portable() {
      ref.setRef(makeGraphWithNonPortableNodes(new NotPortable()));
      try {
        ss = new ServerSocket();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
  }

  @SuppressWarnings("unused")
  private static class NotPortable {
    final Map m = makeGraphWithNonPortableNodes(this);

    Thread    t = Thread.currentThread();

    NotPortable() {
      if (this instanceof Manageable) { throw new AssertionError("this type should not be portable"); }
    }
  }

  private static Map makeGraphWithNonPortableNodes(Object nonPortable) {
    Map m = new LinkedHashMap();
    Ref ref = new Ref();
    Ref r2 = new Ref();
    r2.setRef(nonPortable);
    ref.setRef(r2);

    m.put("ref", ref);

    Object[][] a = new Object[][] { { null }, { new Ref() } };
    ((Ref) a[1][0]).setRef(m);

    m.put("array", a);

    return m;
  }

  private static class LogAppender implements TCAppender {

    private final List events = new ArrayList();

    public void append(LogLevel level, Object message, Throwable throwable) {
      events.add(new Event(level, message, throwable));
    }

    String takeLoggedMessages() {
      StringBuffer buf = new StringBuffer();
      for (Iterator iter = events.iterator(); iter.hasNext();) {
        Event event = (Event) iter.next();
        buf.append(event.getMessage() + "\n");
      }

      events.clear();

      return buf.toString();
    }

  }

  static class Event {
    private final LogLevel  level;
    private final Object    message;
    private final Throwable throwable;

    Event(LogLevel level, Object message, Throwable throwable) {
      this.level = level;
      this.message = message;
      this.throwable = throwable;
    }

    public LogLevel getLevel() {
      return level;
    }

    public Object getMessage() {
      return message;
    }

    public Throwable getThrowable() {
      return throwable;
    }

    @Override
    public String toString() {
      return "[" + level + "] " + message + ((throwable == null) ? "" : throwable.getMessage());
    }
  }

}
