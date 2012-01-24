/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.builtin.ArrayList;
import com.tctest.builtin.ConcurrentHashMap;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.List;
import java.util.Map;

/**
 * See DEV-2912, CDV-1374
 */
public class TxnFoldNewObjectNewTypeTest extends TransparentTestBase {

  public TxnFoldNewObjectNewTypeTest() {
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXOUTSTANDING_BATCHSIZE, "1");
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(2);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier              barrier;
    private final List<Bad>                  badList = new ArrayList<Bad>();
    private static final Map<String, Object> root    = new ConcurrentHashMap<String, Object>(1024);

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());

    }

    @Override
    protected void runTest() throws Throwable {
      int index = barrier.await();

      if (index == 0) {
        produceProblematicFold();
      }

      barrier.await();

      synchronized (badList) {
        Bad bad = badList.iterator().next();
        assertEquals("1", bad.toString());
      }

    }

    private void produceProblematicFold() {
      // get the local txn buffer full-ish so that folding might occur
      for (int i = 0; i < 5000; i++) {
        root.put(String.valueOf(i), new Object());
      }

      Bad bad = new Bad();
      synchronized (badList) {
        badList.add(bad);
      }

      // use a common lock and mutate the field of a "new" instance for a "new" type
      // This mutation will likely be folded into the whole DNA for the Bad instance in the previous transaction
      synchronized (badList) {
        bad.setField(new Value("1"));
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClassName = App.class.getName();

      config.addWriteAutolock("* " + testClassName + ".*(..)");
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("root", "root");
      spec.addRoot("badList", "badList");
      spec.addRoot("barrier", "barrier");

      config.addIncludePattern(Bad.class.getName());
      config.addIncludePattern(Value.class.getName());

    }
  }

  private static class Bad {
    private Value field = new Value("0");

    @Override
    public String toString() {
      return String.valueOf(field);
    }

    void setField(Value value) {
      this.field = value;
    }
  }

  private static class Value {
    private final String val;

    Value(String val) {
      this.val = val;
    }

    @Override
    public String toString() {
      return val;
    }
  }
}
