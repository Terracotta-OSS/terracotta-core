package org.terracotta.express.tests.clusterinfo;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.JMXUtils;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import com.tc.management.beans.L2MBeanNames;
import com.tc.stats.api.DSOClientMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.Callable;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import junit.framework.Assert;

public class RogueClientCoordinator extends ClientBase implements ClusterListener {
  private static final int        MAX_COUNT                    = 1000;

  private final ClusterInfo       cluster;
  private final int               participantCount;
  private final int               totalNoOfPendingTransactionsForClient[];
  private int                     totalNoOfDisconnectedClients = 0;
  private final ToolkitAtomicLong itemsProcessed;
  private final ToolkitBarrier    producerFinished;

  public RogueClientCoordinator(String[] args) {
    super(args);
    this.cluster = getClusteringToolkit().getClusterInfo();
    this.cluster.addClusterListener(this);
    this.participantCount = getParticipantCount();
    this.totalNoOfPendingTransactionsForClient = new int[this.participantCount];
    this.itemsProcessed = getClusteringToolkit().getAtomicLong("test long");
    this.producerFinished = getClusteringToolkit().getBarrier("finished", this.participantCount);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    System.out.println("Co-ordinator waiting for all the clients to get spawned");
    waitForAllClients();

    System.out.println("All Clients got spawned. participantCount " + participantCount);

    int jmxPort = getTestControlMbean().getGroupsData()[0].getJmxPort(0);
    JMXConnector jmxc = JMXUtils.getJMXConnector("localhost", jmxPort);
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
    DSOMBean dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);

    ObjectName[] clientObjectNames = dsoMBean.getClients();
    System.out.println("dsoMBean.getClients() " + dsoMBean.getClients().length);
    DSOClientMBean[] clients = new DSOClientMBean[clientObjectNames.length];
    for (int i = 0; i < clients.length; i++) {
      clients[i] = MBeanServerInvocationHandler.newProxyInstance(mbsc, clientObjectNames[i], DSOClientMBean.class,
                                                                 false);
    }

    while (true) {
      ThreadUtil.reallySleep(500);
      for (int i = 0; i < participantCount; i++) {
        int pendingTransactions = (int) clients[i].getPendingTransactionsCount();
        System.out.println("Client " + i + " Pend Tx Cnt: " + pendingTransactions);
        totalNoOfPendingTransactionsForClient[i] = totalNoOfPendingTransactionsForClient[i] + pendingTransactions;
      }
      if (this.itemsProcessed.get() > MAX_COUNT) break;
    }

    System.out.println("Coordinator ready to kill");
    producerFinished.await();
    System.out.println("Coordinator started killing");
    Assert.assertEquals(this.participantCount, dsoMBean.getClients().length);

    for (int i = 0; i < this.participantCount; i++) {
      if (!clients[i].getNodeID().equals(cluster.getCurrentNode().getId())) {
        clients[i].killClient();
      }
    }

    ThreadUtil.reallySleep(10000);
    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

      @Override
      public Boolean call() throws Exception {
        System.out.println("Waiting until disconnected clients count becomes 4, current: "
                           + totalNoOfDisconnectedClients);
        return 4 == totalNoOfDisconnectedClients;
      }

    });
    System.out.println("All clients killed and verified. Test passed");
    Assert.assertEquals(1, dsoMBean.getClients().length);
    return;
  }

  public void nodeJoined(final ClusterEvent event) {
    // we are not asserting for connection
    // do nothing
  }

  public void nodeLeft(final ClusterEvent event) {
    totalNoOfDisconnectedClients++;
  }

  public void operationsDisabled(final ClusterEvent event) {
    // do nothing
  }

  public void operationsEnabled(final ClusterEvent event) {
    // do nothing
  }

  @Override
  public void onClusterEvent(ClusterEvent event) {
    switch (event.getType()) {
      case OPERATIONS_DISABLED:
        operationsDisabled(event);
        break;
      case OPERATIONS_ENABLED:
        operationsEnabled(event);
        break;
      case NODE_JOINED:
        nodeJoined(event);
        break;
      case NODE_LEFT:
        nodeLeft(event);
        break;

    }

  }
}