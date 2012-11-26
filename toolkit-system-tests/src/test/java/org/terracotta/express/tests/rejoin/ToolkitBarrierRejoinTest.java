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
      ToolkitBarrier afterRejoinBarrier = tk.getBarrier("afterRejoinBarrier", 2);
      int index = afterRejoinBarrier.await();
      debug("client " + index + " starting.. ");
      waitUntilRejoinCompleted();
      debug("client " + index + " done with rejoin");
      doSleep(10);
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
      final ToolkitBarrier rejoinBarrier = tk.getBarrier("rejoinBarrier", 2);
      ToolkitBarrier afterRejoinBarrier = tk.getBarrier("afterRejoinBarrier", 2);
      int index = afterRejoinBarrier.await();
      doDebug("client " + index + " starting.. ");
      final AtomicBoolean exceptionFound = new AtomicBoolean(false);
      Thread anotherThread = new Thread() {
        @Override
        public void run() {
          try {
            doDebug("waiting on toolkitBarrier");
            rejoinBarrier.await(); // 1st hit on rejoinBarrier
          } catch (RejoinInProgressException e) {
            // expected exception
            // exceptionFound.set(true);
          } catch (IllegalMonitorStateException e) {
            // expected exception
            // e.printStackTrace();
            exceptionFound.set(true);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
          }
        }
      };
      anotherThread.start();
      doSleep(5);
      doDebug("done with sleep now doing rejoin");
      startRejoinAndWaitUntilCompleted(testHandlerMBean, tk);
      anotherThread.join();
      doDebug("exceptionFound " + exceptionFound.get());
      if (!exceptionFound.get()) { throw new RuntimeException("RejoinInProgressException should have seen"); }
      rejoinBarrier.await(); // 2nd hit on rejoinBarrier
      debug("client " + index + " done with rejoin");
      afterRejoinBarrier.await();
    }
  }
}