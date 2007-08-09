/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ITransparencyClassSpec;
import com.tc.object.tx.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WaitNotifyUpgradedLocksTestApp extends AbstractTransparentApp {

  private static final int NODES = 2;

  private final Set        set   = new HashSet();
  private final Map        map   = new HashMap();

  public WaitNotifyUpgradedLocksTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

    if (getParticipantCount() != NODES) { throw new RuntimeException("only use " + NODES + " nodes with this test"); }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = WaitNotifyUpgradedLocksTestApp.class.getName();
    ITransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*");
    
    String methodExpression = "* " + testClass + ".run()";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + testClass + "$UpgradeWait.run()";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + testClass + ".runRead(..)";
    config.addAutolock(methodExpression, ConfigLockLevel.READ);

    methodExpression = "* " + testClass + ".runWrite(..)";
    config.addAutolock(methodExpression, ConfigLockLevel.WRITE);

    spec.addRoot("set", "setLock");
    spec.addRoot("map", "mapLock");
  }

  public void run() {
    final boolean first;

    synchronized (set) {
      first = set.size() == 0;
      if (first) set.add(new Object());
    }

    if (first) {
      runUpgrade(map, new UpgradeWait());
    } else {
      synchronized (set) {
        while (set.size() != NODES) {
          try {
            set.wait();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      }

      runUpgrade(map, new Runnable() {
        public void run() {
          map.notify();
        }
      });
    }
  }

  public void runUpgrade(final Map m, final Runnable action) {
    runRead(m, new Runnable() {
      public void run() {
        runWrite(m, action);
      }
    });
  }

  public void runRead(Map m, Runnable action) {
    synchronized (m) {
      action.run();

      try {
        m.put(new Object(), null);
        throw new RuntimeException("Not in a read only context");
      } catch (ReadOnlyException r) {
          // expected
      }
    }
  }

  public void runWrite(Map m, Runnable action) {
    synchronized (m) {
      action.run();
    }
  }

  private class UpgradeWait implements Runnable {
    public void run() {
      // notify the other thread, but only after holding the upgrade on "map"
      synchronized (set) {
        set.add(new Object());
        set.notify();
      }

      try {
        map.wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
