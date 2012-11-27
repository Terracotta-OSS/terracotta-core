/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ToolkitBarrierRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitBarrierRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitBarrierRejoinTestClient.class, BarrierTestClient.class);
  }

  public static class BarrierTestClient extends AbstractToolkitRejoinTestClient {

    public BarrierTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal tk = createRejoinToolkit();
      ToolkitBarrier beforeRejoinBarrier = tk.getBarrier("beforeRejoinBarrier", 2);
      int index = beforeRejoinBarrier.await();
      debug("client " + index + " starting.. ");
      waitUntilRejoinCompleted();
      debug("client " + index + " done with rejoin");
      beforeRejoinBarrier.await();
      ToolkitBarrier afterRejoinBarrier = tk.getBarrier("afterRejoinBarrier", 2);
      debug("client " + index + " waiting on " + afterRejoinBarrier.getName());
      afterRejoinBarrier.await();
    }
  }

  public static class ToolkitBarrierRejoinTestClient extends AbstractToolkitRejoinTestClient {

    public ToolkitBarrierRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal tk = createRejoinToolkit();
      final ToolkitBarrier duringRejoinBarrier = tk.getBarrier("duringRejoinBarrier", 2);
      final ToolkitBarrier beforeRejoinBarrier = tk.getBarrier("beforeRejoinBarrier", 2);
      int index = beforeRejoinBarrier.await();
      doDebug("client " + index + " starting.. ");
      final AtomicBoolean exceptionFound = new AtomicBoolean(false);
      Thread anotherThread = new Thread() {
        @Override
        public void run() {
          try {
            doDebug("waiting on toolkitBarrier");
            duringRejoinBarrier.await(); // 1st hit on duringRejoinBarrier
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
          } catch (Exception e) {
            if (e.getClass().getName().equals("com.tc.exception.RejoinInProgressException")) {
              exceptionFound.set(true);
            }
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
      debug("client " + index + " done with rejoin");
      beforeRejoinBarrier.await();
      ToolkitBarrier afterRejoinBarrier = tk.getBarrier("afterRejoinBarrier", 2);
      debug("client " + index + " waiting on " + afterRejoinBarrier.getName());
      afterRejoinBarrier.await();
    }
  }
}