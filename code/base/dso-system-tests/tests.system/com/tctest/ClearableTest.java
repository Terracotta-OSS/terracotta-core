/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TCMap;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ClearableTest extends TransparentTestBase {

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  private static Map makeCHM() throws Exception {
    return (Map) Class.forName("java.util.concurrent.ConcurrentHashMap").newInstance();
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private static final int NUM        = 100;
    private final List       clearables = new ArrayList();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    protected void runTest() throws Throwable {
      add(new HashMap());
      add(new Hashtable());

      if (Vm.isJDK15Compliant()) {
        add(makeCHM());
      }

      for (Iterator i = clearables.iterator(); i.hasNext();) {
        Clearable c = (Clearable) i.next();

        // At the time this test was written, all Clearable types should have eviction enabled by default
        // (ie. one should not need to manually enable eviction)
        if (!c.isEvictionEnabled()) { throw new RuntimeException(c.getClass() + " does not have eviction enabled"); }

        populate(c);
        clear(c);
        validateCleared(c);
      }
    }

    private void validateCleared(Clearable c) {
      if (c instanceof TCMap) {
        Collection local = ((TCMap) c).__tc_getAllLocalEntriesSnapshot();
        Assert.assertEquals(c.getClass(), 0, local.size());

        for (Iterator i = ((Map) c).entrySet().iterator(); i.hasNext();) {
          Map.Entry entry = (Entry) i.next();
          Key k = (Key) entry.getKey();
          Value v = (Value) entry.getValue();
          Assert.assertEquals(k.getI(), v.getI());
        }

      } else {
        throw new RuntimeException("no validateCleared support for " + c.getClass());
      }
    }

    private void clear(Clearable c) {
      if (c instanceof Map) {
        Map m = (Map) c;
        for (Iterator i = m.values().iterator(); i.hasNext();) {
          ((Manageable) i.next()).__tc_managed().clearAccessed();
        }

        int numToClear = m.size();
        int cleared = c.__tc_clearReferences(numToClear);
        Assert.assertEquals(c.getClass(), numToClear, cleared);
      } else {
        throw new RuntimeException("no clear support for " + c.getClass());
      }
    }

    private void populate(Object o) {
      if (o instanceof Map) {
        synchronized (o) {
          for (int i = 0; i < NUM; i++) {
            ((Map) o).put(new Key(i), new Value(i));
          }
        }
      } else {
        throw new RuntimeException("no populate support for " + o.getClass());
      }
    }

    private void add(Object clearable) {
      if (!(clearable instanceof Clearable)) { throw new RuntimeException(clearable.getClass() + " is not Clearable!"); }
      synchronized (clearables) {
        clearables.add(clearable);
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("clearables", "clearables");
      String methodExpression = "* " + testClassName + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      config.addIncludePattern(Key.class.getName());
      config.addIncludePattern(Value.class.getName());
    }

  }

  private static class Key {

    private final int i;

    Key(int i) {
      this.i = i;
    }

    int getI() {
      return i;
    }

    public int hashCode() {
      return i;
    }

    public boolean equals(Object obj) {
      if (obj instanceof Key) { return this.i == ((Key) obj).i; }
      return false;
    }

  }

  private static class Value {

    private final int i;

    Value(int i) {
      this.i = i;
    }

    int getI() {
      return i;
    }

  }

}
