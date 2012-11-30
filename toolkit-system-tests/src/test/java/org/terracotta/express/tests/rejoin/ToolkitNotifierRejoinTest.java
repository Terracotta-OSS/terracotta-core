/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.events.ToolkitNotificationEvent;
import org.terracotta.toolkit.events.ToolkitNotificationListener;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;

import junit.framework.Assert;

public class ToolkitNotifierRejoinTest extends AbstractToolkitRejoinTest {
  public ToolkitNotifierRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitNotifierRejoinTestClient.class, ToolkitNotifierRejoinTestClient.class);
  }

  public static class ToolkitNotifierRejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int    NO_OF_NODES       = 2;
    private static final String MSG_BEFORE_REJOIN = "Before_rejoin:";
    private static final String MSG_AFTER_REJOIN  = "After_rejoin:";
    private static final String NOTIFIER_NAME     = "someNotifier";

    public ToolkitNotifierRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal toolkit = createRejoinToolkit();
      doDebug("Toolkit Created");
      ToolkitNotifier<String> notifier = toolkit.getNotifier(NOTIFIER_NAME, String.class);
      ToolkitBarrier barrier = toolkit.getBarrier("SomeBarrier", NO_OF_NODES);
      final int nodeIndex = barrier.await();
      doDebug("client " + nodeIndex + " Notifier Created");

      CountDownLatch latch = new CountDownLatch(1);
      final MyNotificationListener listener = new MyNotificationListener(latch);
      notifier.addNotificationListener(listener);
      doDebug("listener ADDED");

      sendAndReceiveNotification(notifier, barrier, nodeIndex, latch, listener, MSG_BEFORE_REJOIN);
      sendAndReceiveNotification(notifier, barrier, nodeIndex, latch, listener, MSG_BEFORE_REJOIN + MSG_AFTER_REJOIN);

      barrier.await();

      doDebug("intiating  rejoin..");
      if (nodeIndex == 0) {
        startRejoinAndWaitUntilCompleted(testHandlerMBean, toolkit);
      } else {
        waitUntilRejoinCompleted();
      }

      barrier.await();
      doDebug("rejoin completed : nodeIndex= " + nodeIndex);

      sendAndReceiveNotification(notifier, barrier, nodeIndex, latch, listener, MSG_AFTER_REJOIN);

    }

    private void sendAndReceiveNotification(ToolkitNotifier<String> notifier, ToolkitBarrier barrier,
                                            final int nodeIndex, CountDownLatch latch,
                                            final MyNotificationListener listener, String message)
        throws InterruptedException,
        BrokenBarrierException {
      if (nodeIndex == 1) {
        doDebug("Notifying all listeners");
        notifier.notifyListeners(message);
      } else {
        doDebug("waiting till other node Notifies all my listeners");
      }

      barrier.await();

      if (nodeIndex == 0) {
        doDebug("node 0 is asserting somethings");
        latch.await();
        Assert.assertEquals(message, listener.getMsgRecvd());
        Assert.assertEquals(NOTIFIER_NAME, notifier.getName());
        Assert.assertNotNull(listener.getRemoteNode());
        Assert.assertEquals(1, notifier.getNotificationListeners().size());
      } else {
        doDebug("node 1 is asserting somethings");
        Assert.assertNull(listener.getRemoteNode());
        Assert.assertNull(listener.getMsgRecvd());
      }
    }

  }

  public static class MyNotificationListener implements ToolkitNotificationListener<String> {
    private final CountDownLatch latch;
    private volatile ClusterNode remoteNode;
    private volatile String      msgRecvd;

    public MyNotificationListener(CountDownLatch latch) {
      this.latch = latch;
    }

    public ClusterNode getRemoteNode() {
      return remoteNode;
    }

    public String getMsgRecvd() {
      return msgRecvd;
    }

    @Override
    public void onNotification(ToolkitNotificationEvent event) {
      // Assert that the notification is done is Executor Thread not in Stage thread.
      System.err.println("Got notified " + event + " Thread : " + Thread.currentThread().getName());
      Assert.assertTrue(Thread.currentThread().getName().contains("ToolkitNotifier"));
      msgRecvd = (String) event.getMessage();
      remoteNode = event.getRemoteNode();
      latch.countDown();
    }
  }

}
