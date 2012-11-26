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

import java.util.concurrent.CountDownLatch;

import junit.framework.Assert;

public class ToolkitNotifierRejoinTest extends AbstractToolkitRejoinTest {
  public ToolkitNotifierRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitNotifierRejoinTestClient.class, ToolkitNotifierRejoinTestClient.class);
  }

  public static class ToolkitNotifierRejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int    No_OF_NODES       = 2;
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
      ToolkitBarrier barrier = toolkit.getBarrier("SomeBarrier", No_OF_NODES);
      doDebug("Notifier Created");

      CountDownLatch latch = new CountDownLatch(1);
      MyNotificationListener listener = new MyNotificationListener(latch);
      notifier.addNotificationListener(listener);
      doDebug("listener ADDED");

      final int nodeIndex = barrier.await();

      if (nodeIndex == 1) {
        doDebug("Notifying all listeners");
        notifier.notifyListeners(MSG_BEFORE_REJOIN);
      } else {
        doDebug("waiting till other node Notifies all my listeners");
      }

      if (nodeIndex == 0) {
        doDebug("0 reached");
      } else {
        doDebug("1 reached");
      }
      barrier.await();

      if (nodeIndex == 0) {
        doDebug("0 reached out");
      } else {
        doDebug("1 reached out");
      }

      if (nodeIndex == 0) {
        doDebug("node 0 is asserting somethings");
        latch.await();
        Assert.assertEquals(MSG_BEFORE_REJOIN, listener.getMsgRecvd());
        Assert.assertEquals(NOTIFIER_NAME, notifier.getName());
        Assert.assertNotNull(listener.getRemoteNode());
        Assert.assertEquals(1, notifier.getNotificationListeners().size());
      } else {
        doDebug("node 1 is asserting somethings");
        Assert.assertNull(listener.getRemoteNode());
        Assert.assertNull(listener.getMsgRecvd());
      }

      barrier.await();

      if (nodeIndex == 0) {
        doDebug("client 0 is trying to intiate rejoin..");
        startRejoinAndWaitUntilCompleted(testHandlerMBean, toolkit);
      } else {
        doDebug("Client1: other client is trying to initiate rejoin");
      }

      barrier.await();

      doDebug("rejoin done");
      doDebug("rejoin completed : nodeIndex= " + nodeIndex);

      if (nodeIndex == 0) {
        doDebug("Sending notification to all listener,  Rejoin");
        notifier.notifyListeners(MSG_AFTER_REJOIN);
      }
      barrier.await();

      if (nodeIndex == 0) {
        doDebug("node 0 is asserting somethings");
        latch.await();
        Assert.assertEquals(MSG_BEFORE_REJOIN, listener.getMsgRecvd());
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
