/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.exception.RejoinInProgressException;
import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.util.concurrent.BrokenBarrierException;

import junit.framework.Assert;

public class ToolkitBarrierRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitBarrierRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitBarrierRejoinTestClient.class);
  }

  public static class ToolkitBarrierRejoinTestClient extends AbstractToolkitRejoinTestClient {

    public ToolkitBarrierRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal tk = createRejoinToolkit();
      final ToolkitBarrier toolkitBarrier = tk.getBarrier("rejoinBarrier", 2);

      Thread anotherThread = new Thread() {
        @Override
        public void run() {
          try {
            doDebug("waiting on toolkitBarrier");
            toolkitBarrier.await();
            Assert.fail("should have failed with RejoinInProgressException");
          } catch (RejoinInProgressException e) {
            // expected exception
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
          }
        }
      };
      anotherThread.start();
      doSleep(5);
      doDebug("done with sleep now starting rejoin");
      startRejoinAndWaitUnilCompleted(testHandlerMBean, tk);
      anotherThread.join();
    }
  }
}