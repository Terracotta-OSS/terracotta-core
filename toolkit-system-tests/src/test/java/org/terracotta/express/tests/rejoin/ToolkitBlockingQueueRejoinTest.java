/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.util.NonLiteralKeyNonLiteralValueGenerator;
import org.terracotta.express.tests.util.TCInt;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

public class ToolkitBlockingQueueRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitBlockingQueueRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitBlockingQueueRejoinTestClient.class, ToolkitBlockingQueueRejoinTestClient.class);
    testConfig.setRestartable(false);
  }

  public static class ToolkitBlockingQueueRejoinTestClient extends AbstractToolkitRejoinTestClient {
    private static final int NUM_ELEMENTS = 1000;

    public ToolkitBlockingQueueRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal tk = createRejoinToolkit();
      keyValueGenerator = new NonLiteralKeyNonLiteralValueGenerator();
      ToolkitBarrier toolkitBlockingQueueBarrier = tk.getBarrier("toolkitBlockingQueueBarrier", 2);
      int index = toolkitBlockingQueueBarrier.await();
      final ToolkitBlockingQueue<TCInt> toolkitBlockingQueue = tk.getBlockingQueue("toolkitBlockingQueue", TCInt.class);

      doDebug("client " + index + " starting.. ");
      if (index == 0) {
        doDebug("Adding values before rejoin");
        for (int i = 0; i < NUM_ELEMENTS; i++) {
          toolkitBlockingQueue.add((TCInt) keyValueGenerator.getValue(i));
        }
        testSingleNode(testHandlerMBean, tk, index);
      } else {
        waitUntilRejoinCompleted();
      }

      debug("client " + index + " done with rejoin");
      toolkitBlockingQueueBarrier.await();

      doDebug("Asserting old values after rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertTrue(toolkitBlockingQueue.contains(keyValueGenerator.getValue(i)));
      }
      toolkitBlockingQueueBarrier.await();

      if (index == 1) {
        doDebug("Adding new values after rejoin");
        for (int i = NUM_ELEMENTS; i < 2 * NUM_ELEMENTS; i++) {
          toolkitBlockingQueue.add((TCInt) keyValueGenerator.getValue(i));
        }
      }
      toolkitBlockingQueueBarrier.await();

      doDebug("Asserting new values after rejoin");
      for (int i = 0; i < 2 * NUM_ELEMENTS; i++) {
        Assert.assertTrue(toolkitBlockingQueue.contains(keyValueGenerator.getValue(i)));
      }

      doDebug("getting a fresh blocking queue after rejoin");
      ToolkitBlockingQueue<TCInt> freshToolkitBlockingQueue = tk.getBlockingQueue("freshToolkitBlockingQueue",
                                                                                  TCInt.class);
      if (index == 1) {
        doDebug("adding values in fresh blocking queue after rejoin");
        for (int i = 0; i < NUM_ELEMENTS; i++) {
          freshToolkitBlockingQueue.add((TCInt) keyValueGenerator.getValue(i));
        }
        tk.waitUntilAllTransactionsComplete();
        toolkitBlockingQueueBarrier.await();
      } else {
        toolkitBlockingQueueBarrier.await();
        doDebug("asserting fresh blocking queue after rejoin " + freshToolkitBlockingQueue.size());
        for (int i = 0; i < NUM_ELEMENTS; i++) {
          Assert.assertTrue(freshToolkitBlockingQueue.contains(keyValueGenerator.getValue(i)));
        }
      }
    }

    private void testSingleNode(TestHandlerMBean testHandlerMBean, ToolkitInternal tk, int index) throws Exception {
      final ToolkitBlockingQueue<TCInt> takeTBQueue = tk.getBlockingQueue("takeTBQueue", 1, TCInt.class);
      final ToolkitBlockingQueue<TCInt> putTBQueue = tk.getBlockingQueue("putTBQueue", 1, TCInt.class);
      putTBQueue.put((TCInt) keyValueGenerator.getValue(0)); // one dummy value already added, not next put should be
                                                             // blocked
      final AtomicBoolean exceptionPutFound = new AtomicBoolean(false);
      final AtomicBoolean exceptionTakeFound = new AtomicBoolean(false);
      Thread putThread = startThread(putTBQueue, exceptionPutFound, "put");
      Thread takeThread = startThread(takeTBQueue, exceptionTakeFound, "take");

      doSleep(20);
      doDebug("done with sleep now doing rejoin");
      startRejoinAndWaitUntilCompleted(testHandlerMBean, tk);
      takeThread.join();
      putThread.join();
      doDebug("exceptionPutFound " + exceptionPutFound.get() + " exceptionTakeFound " + exceptionTakeFound.get());
      if (!exceptionPutFound.get()) { throw new RuntimeException("PUT - RejoinInProgressException should have seen"); }
      if (!exceptionTakeFound.get()) { throw new RuntimeException("TAKE - RejoinInProgressException should have seen"); }

    }

    private Thread startThread(final ToolkitBlockingQueue<TCInt> toolkitBlockingQueue,
                               final AtomicBoolean exceptionFound, final String operation) {
      Thread anotherThread = new Thread() {
        @Override
        public void run() {
          try {
            doDebug(toolkitBlockingQueue.getName() + " " + operation + " " + exceptionFound.get());
            if(operation.equals("put")) {
              toolkitBlockingQueue.put((TCInt) keyValueGenerator.getValue(0));
              doDebug("PUT did not block");
            }
            if(operation.equals("take")) {
              toolkitBlockingQueue.take();
              doDebug("TAKE did not block");
            }
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          } catch (RejoinException e) {
            exceptionFound.set(true);
          }
        }
      };
      anotherThread.start();
      return anotherThread;
    }

  }

}
