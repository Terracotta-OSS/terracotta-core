/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.LinkedHashMap;
import java.util.Map;

public class NullLiteralReferencesTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return NullLiteralReferencesTestApp.class;
  }

  public static class NullLiteralReferencesTestApp extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier barrier;
    private final Map           root = new LinkedHashMap();

    public NullLiteralReferencesTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    @Override
    protected void runTest() throws Throwable {
      int index = barrier.barrier();

      if (index == 0) {
        Holder h1 = new Holder(true);
        Holder h2 = new Holder(false);

        // these two put()'s need to be done in separate TXNs to ensure that
        // the state object on the server be deterministic. More over, the
        // version that has non-null reference literals (h2) must be in the
        // first transaction
        synchronized (root) {
          root.put("null", h2);
        }

        synchronized (root) {
          root.put("not-null", h1);
        }

        h1.setNonNull();
        h2.setNull();
      }

      barrier.barrier();

      Holder compareNull = new Holder(true);
      Holder compareNotNull = new Holder(false);

      synchronized (root) {
        Assert.assertEquals(compareNull, root.get("null"));
        Assert.assertEquals(compareNotNull, root.get("not-null"));
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      new CyclicBarrierSpec().visit(visitor, config);

      String testClass;
      TransparencyClassSpec spec;
      String methodExpression;

      testClass = Holder.class.getName();
      spec = config.getOrCreateSpec(testClass);
      methodExpression = "* " + testClass + ".*(..)";
      config.addWriteAutolock(methodExpression);

      testClass = NullLiteralReferencesTestApp.class.getName();
      spec = config.getOrCreateSpec(testClass);
      methodExpression = "* " + testClass + ".*(..)";
      config.addWriteAutolock(methodExpression);
      spec.addRoot("barrier", "barrier");
      spec.addRoot("root", "root");
    }
  }

  @SuppressWarnings("unused")
  private static class Holder {
    Holder(boolean setNull) {
      if (setNull) {
        setNull();
      } else {
        setNonNull();
      }
    }

    Byte              byteRef;
    Boolean           booleanRef;
    Character         characterRef;
    Double            doubleRef;
    Float             floatRef;
    Integer           integerRef;
    Long              longRef;
    Short             shortRef;

    Class             clazz;
    StackTraceElement stack;
    String            str;

    byte              b = 1;
    boolean           z = true;
    char              c = 'e';
    double            d = Math.PI;
    float             f = "floater".length();
    int               i = -9;
    long              l = 2342342344324234L;
    short             s = 3;

    synchronized void setNonNull() {
      byteRef = new Byte((byte) 1);
      booleanRef = new Boolean(true);
      characterRef = new Character('q');
      doubleRef = new Double(3.14);
      floatRef = new Float(2.78);
      integerRef = new Integer(42);
      longRef = new Long(666);
      shortRef = new Short((short) "steve".length());

      clazz = getClass();
      stack = new Throwable().getStackTrace()[0];
      str = "timmy";
    }

    synchronized void setNull() {
      byteRef = null;
      booleanRef = null;
      characterRef = null;
      doubleRef = null;
      floatRef = null;
      integerRef = null;
      longRef = null;
      shortRef = null;

      clazz = null;
      stack = null;
      str = null;
    }

    // this method sync'd to have a common shared memory barrier with the mutate methods
    @Override
    public synchronized boolean equals(Object o) {
      return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public synchronized String toString() {
      return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public synchronized int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
    }

  }

}
