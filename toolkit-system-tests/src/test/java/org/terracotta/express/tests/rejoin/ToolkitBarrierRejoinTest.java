/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ToolkitBarrierRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitBarrierRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitBarrierRejoinTestClient.class, ToolkitBarrierRejoinTestClient.class);
    testConfig.setRestartable(false);
  }

  public static class ToolkitBarrierRejoinTestClient extends AbstractToolkitRejoinTestClient {

    public ToolkitBarrierRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal tk = createRejoinToolkit();
      ToolkitBarrier beforeRejoinBarrier = tk.getBarrier("beforeRejoinBarrier", 2);
      int index = beforeRejoinBarrier.await();
      doDebug("client " + index + " starting.. ");
      if (index == 0) {
        testSingleNode(testHandlerMBean, tk, index);
      } else {
        waitUntilRejoinCompleted();
      }
      debug("client " + index + " done with rejoin");
      beforeRejoinBarrier.await();
      ToolkitBarrier afterRejoinBarrier = tk.getBarrier("afterRejoinBarrier", 2);
      debug("client " + index + " waiting on " + afterRejoinBarrier.getName());
      afterRejoinBarrier.await();
    }

    private void testSingleNode(TestHandlerMBean testHandlerMBean, ToolkitInternal tk, final int index)
        throws Exception,
        InterruptedException, BrokenBarrierException {
      final ToolkitBarrier duringRejoinBarrier = tk.getBarrier("duringRejoinBarrier", 2);
      final AtomicBoolean exceptionFound = new AtomicBoolean(false);
      Thread anotherThread = new Thread() {
        @Override
        public void run() {
          try {
            doDebug("client " + index + " waiting on " + duringRejoinBarrier.getName());
            duringRejoinBarrier.await(); // 1st hit on duringRejoinBarrier
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
          } catch (RejoinException e) {
            exceptionFound.set(true);
          }
        }
      };
      anotherThread.start();
      doSleep(10);
      doDebug("done with sleep now doing rejoin");
      startRejoinAndWaitUntilCompleted(testHandlerMBean, tk);
      anotherThread.join();
      doDebug("exceptionFound " + exceptionFound.get());
      if (!exceptionFound.get()) { throw new RuntimeException("RejoinInProgressException should have seen"); }
      duringRejoinBarrier.await(); // 2nd hit on duringRejoinBarrier
    }
  }
}
