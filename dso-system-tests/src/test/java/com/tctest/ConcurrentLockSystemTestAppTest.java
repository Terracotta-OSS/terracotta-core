/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.simulator.listener.ListenerProvider;
import com.tc.simulator.listener.MockStatsListener;
import com.tc.simulator.listener.MutationCompletionListener;
import com.tc.simulator.listener.OutputListener;
import com.tc.simulator.listener.ResultsListener;
import com.tc.simulator.listener.StatsListener;
import com.tctest.runner.TransparentAppConfig;

import java.util.Properties;

import junit.framework.TestCase;

public class ConcurrentLockSystemTestAppTest extends TestCase {

  private TransparentAppConfig cfg;

  public void setUp() throws Exception {
    cfg = new TransparentAppConfig(ConcurrentLockSystemTestApp.class.getName(), null, 1, 1, null);
  }

  public void testBasics() {
    ConcurrentLockSystemTestApp app = new ConcurrentLockSystemTestApp("test", cfg, new ListenerProvider() {

      public OutputListener getOutputListener() {
        return null;
      }

      public ResultsListener getResultsListener() {
        return null;
      }

      public StatsListener newStatsListener(Properties properties) {
        return new MockStatsListener();
      }

      public MutationCompletionListener getMutationCompletionListener() {
        return null;
      }

    });
    app.run();
  }
}
