/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.longrunning;

import EDU.oswego.cs.dl.util.concurrent.CountDown;

import com.tc.net.proxy.TCPProxy;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.MockListenerProvider;
import com.tc.simulator.listener.MockOutputListener;
import com.tc.simulator.listener.MockResultsListener;
import com.tc.simulator.listener.MockStatsListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

public class LargeGraphTestAppTest extends TestCase {

  private ApplicationConfig    cfg;
  private MockListenerProvider listeners;

  public void setUp() throws Exception {
    super.setUp();
    cfg = new ApplicationConfig() {

      public String getApplicationClassname() {
        return LargeGraphTestApp.class.getName();
      }

      public void setAttribute(String key, String value) {
        //
      }

      public String getAttribute(String key) {
        return null;
      }

      public int getIntensity() {
        throw new AssertionError();
      }

      public int getGlobalParticipantCount() {
        throw new AssertionError();
      }

      public ApplicationConfig copy() {
        throw new AssertionError();
      }

      public ServerControl getServerControl() {
        throw new AssertionError();
      }

      public int getValidatorCount() {
        throw new AssertionError();
      }

      public int getGlobalValidatorCount() {
        throw new AssertionError();
      }

      public TCPProxy[] getProxies() {
        throw new AssertionError();
      }

      public ServerControl[] getServerControls() {
        throw new AssertionError();
      }

      public Object getAttributeObject(String key) {
        throw new AssertionError();
      }
    };
    listeners = new MockListenerProvider();
    listeners.outputListener = new MockOutputListener();
    listeners.resultsListener = new MockResultsListener();
    listeners.statsListener = new MockStatsListener();
  }

  private void test(LargeGraphTestApp application, int objectCount) throws Exception {
    application.growGraph(objectCount, 50);
    assertEquals(objectCount, application.getObjectCount());
    if (LargeGraphTestApp.doVerify()) {
      application.verifyGraph();
      application.verifyReferences();
    }
  }

  public void testBasic() throws Exception {
    test(new LargeGraphTestApp("yer app id", cfg, listeners), 100);
  }

  public void testConcurrent() throws Throwable {
    int threads = 10;
    final CountDown countdown = new CountDown(threads);
    final Random random = new Random();
    final List errors = new ArrayList();
    for (int i = 0; i < threads; i++) {
      final String id = i + "";
      Thread t = new Thread(new Runnable() {

        public void run() {
          for (int j = 0; j < 10; j++) {
            final LargeGraphTestApp app = new LargeGraphTestApp(id + j, cfg, listeners);
            int objectCount = random.nextInt(10) * 1000;
            if (objectCount == 0) objectCount = 1000;
            try {
              test(app, objectCount);
            } catch (Throwable e) {
              errors.add(e);
              countdown.release();
              return;
            }
          }
          countdown.release();
        }

      });
      t.start();
    }
    countdown.acquire();
    for (Iterator i = errors.iterator(); i.hasNext();) {
      ((Throwable) i.next()).printStackTrace();
    }
    if (errors.size() > 0) { throw (Throwable) errors.get(0); }
  }

}