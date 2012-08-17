/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.clusterinfo;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

public class RogueClientTest extends AbstractToolkitTestBase {

  public RogueClientTest(TestConfig testConfig) {
    super(testConfig, RogueClientCoordinator.class, RogueClientTestClient.class, RogueClientTestClient.class,
          RogueClientTestClient.class, RogueClientTestClient.class);
  }

  @Override
  protected void startClients() throws Throwable {
    final int numOfClients = 5;
    final AtomicBoolean passed = new AtomicBoolean(true);
    Thread[] threads = new Thread[numOfClients];

    threads[0] = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          runClient(RogueClientCoordinator.class);
        } catch (Throwable e) {
          e.printStackTrace();
          passed.set(false);
        }
      }
    }, "coordinator thread");

    for (int i = 1; i < numOfClients; i++) {
      threads[i] = new Thread(new Runnable() {

        @Override
        public void run() {
          try {
            runClient(RogueClientTestClient.class);
          } catch (Throwable e) {
            e.printStackTrace();
            passed.set(false);
          }
        }
      }, "client thread " + i);
    }

    for (Thread th : threads)
      th.start();

    threads[0].join();
    Assert.assertTrue(passed.get());
  }

}
