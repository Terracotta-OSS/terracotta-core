/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

public class ClientDetectionTest extends AbstractToolkitTestBase {
  private final AtomicBoolean passed = new AtomicBoolean(true);

  public ClientDetectionTest(TestConfig testConfig) {
    super(testConfig);
  }

  @Override
  protected void startClients() throws Throwable {
    // start coordinator
    Thread coordinator = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          runClient(ClientDetectionTestApp.class);
        } catch (Throwable e) {
          e.printStackTrace();
          passed.set(false);
        }

      }
    }, "test coordinator");
    coordinator.start();

    spawnAllClients();

    coordinator.join();
    Assert.assertTrue(passed.get());
  }

  private void spawnAllClients() {
    for (int i = 0; i < ClientDetectionL1Client.NUM_OF_CLIENTS; i++) {
      Thread th = new Thread(new Runnable() {

        @Override
        public void run() {
          try {
            runClient(ClientDetectionL1Client.class);
          } catch (Throwable e) {
            e.printStackTrace();
            passed.set(false);
          }
        }
      }, "test client");
      th.start();
    }

  }

}
