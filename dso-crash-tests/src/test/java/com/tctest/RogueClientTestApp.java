/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.exception.TCRuntimeException;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.stats.api.DSOClientMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.builtin.AtomicInteger;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public class RogueClientTestApp extends AbstractTransparentApp {

  private static final int                         MAX_COUNT        = 1000;
  private static final int                         TOTAL_L1_PROCESS = 5;

  private final CyclicBarrier                      barrier          = new CyclicBarrier(TOTAL_L1_PROCESS);
  private final CyclicBarrier                      finished         = new CyclicBarrier(TOTAL_L1_PROCESS - 2);
  private static final LinkedBlockingQueue<MyNode> lbqueue          = new LinkedBlockingQueue<MyNode>();
  private static final AtomicInteger               nodeId           = new AtomicInteger(0);

  public static final String                       CONFIG_FILE      = "config-file";
  public static final String                       PORT_NUMBER      = "port-number";
  public static final String                       HOST_NAME        = "host-name";
  public static final String                       JMX_PORT         = "jmx-port";
  public static final int                          TYPE_CONSUMER    = 1;
  public static final int                          TYPE_PRODUCER    = 2;

  private final ApplicationConfig                  appConfig;
  private JMXConnector                             jmxc;
  private MBeanServerConnection                    mbsc;
  private DSOMBean                                 dsoMBean;
  private ExtraL1ProcessControl                    client;

  public RogueClientTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    appConfig = cfg;
  }

  public void run() {
    for (int i = 1; i <= TOTAL_L1_PROCESS; i++) {
      testRogueClients(i);
    }
    return;
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = RogueClientTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*", false, false, true);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("finished", "finished");
    spec.addRoot("lbqueue", "lbqueue");
    spec.addRoot("nodeId", "nodeId");
  }

  private void testRogueClients(final int nodeRunner) {

    if (nodeRunner == TOTAL_L1_PROCESS) {
      RogueClientCoordinator clientCoordinator = new RogueClientCoordinator();
      clientCoordinator.startRogueClientCoordinator();
    } else {
      startCPUPegger();
      try {
        final String hostName = appConfig.getAttribute(HOST_NAME);
        final int portNumber = Integer.parseInt(appConfig.getAttribute(PORT_NUMBER));
        final File configFile = new File(appConfig.getAttribute(CONFIG_FILE));
        File workingDir = new File(configFile.getParentFile(), "l1client" + nodeRunner);
        List jvmArgs = new ArrayList();
        jvmArgs.add("-Dtc.node-name=node" + nodeRunner);
        jvmArgs.add("-Dtc.config=" + configFile.getAbsolutePath());
        // System.out.println("-Dtc.config=" + configFile.getAbsolutePath());
        FileUtils.forceMkdir(workingDir);

        if (nodeRunner % 2 == 0) {
          // make a producer
          client = new ExtraL1ProcessControl(hostName, portNumber, L1Client.class, configFile.getAbsolutePath(),
                                             Arrays.asList("" + nodeRunner, "" + TYPE_PRODUCER), workingDir, jvmArgs);
          client.start();
        } else {
          // make a consumer
          client = new ExtraL1ProcessControl(hostName, portNumber, L1Client.class, configFile.getAbsolutePath(),
                                             Arrays.asList("" + nodeRunner, "" + TYPE_CONSUMER), workingDir, jvmArgs);
          client.start();
        }
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static class L1Client {
    private final int     clientId;
    private final int     type;

    // redeclaring so that the barriers get shared between the L1s and the coordinator
    private CyclicBarrier barrier;
    private CyclicBarrier finished;

    public L1Client(final int id, final int type) {
      this.clientId = id;
      this.type = type;
    }

    public static void main(final String args[]) {
      if (args.length != 2) { throw new AssertionError("Usage : Client <id> <type>"); }

      L1Client client = new L1Client(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
      client.execute();
    }

    public void execute() {
      try {
        System.out.println("Client number " + this.clientId + " is entering into the barrier");
        barrier.await();
      } catch (Exception e) {
        throw new AssertionError(e);
      }

      System.out.println("Client number " + this.clientId + " has been spawned, waiting for others to get spawned");

      if (this.type == TYPE_CONSUMER) {
        consume();
      } else if (this.type == TYPE_PRODUCER) {
        produce();
        try {
          finished.await();
        } catch (Exception e) {
          throw new AssertionError(e);
        }
      } else {
        throw new AssertionError("Usage : Client <id> <type>, Please pass the arguments correctly");
      }
      try {
        System.out.println("Client " + this.clientId + " finished, waiting to be killed");
        // wait indefinitly, coordinator will kill u
        Thread.currentThread().join(Long.MAX_VALUE);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }

    private void consume() {
      while (true) {
        try {
          // let consumer be little slow
          ThreadUtil.reallySleep(50);
          MyNode myNode = lbqueue.poll();

          if (myNode == null) {
            if (getCurrentNodeID() >= MAX_COUNT) return;
            continue;
          }
          int id = myNode.getId();
          if (id % 20 == 0) System.out.println("Clinet " + this.clientId + " consumed node number " + id);
          if (id > MAX_COUNT) break;
        } catch (Exception e) {
          throw new AssertionError(e);
        }
      }
    }

    private void produce() {
      while (true) {
        int id = getNextNodeID();
        if (id > MAX_COUNT) break;
        MyNode myNode = new MyNode(id);
        try {
          lbqueue.put(myNode);
          if (id % 20 == 0) System.out.println("Clinet " + this.clientId + " produced node number " + id);
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    }

    private synchronized static int getNextNodeID() {
      return nodeId.incrementAndGet();
    }

    private synchronized static int getCurrentNodeID() {
      return nodeId.get();
    }

  }

  private void startCPUPegger() {
    // start the thread to peg the cpu
    Thread pegCPUThread = new Thread(new PegCPU(), "CPU Pegger");
    pegCPUThread.start();
  }

  /**
   * Coordinator
   */

  private class RogueClientCoordinator implements DsoClusterListener {

    @InjectedDsoInstance
    private DsoCluster cluster;

    int                participantCount                        = TOTAL_L1_PROCESS;
    private final int  totalNoOfPendingTransactionsForClient[] = new int[participantCount];
    private int        totalNoOfDisconnectedClients            = 0;

    public void startRogueClientCoordinator() {
      try {
        System.out.println("Co-ordinator waiting for all the clients to get spawned");
        barrier.await();
      } catch (Exception e) {
        throw new AssertionError(e);
      }

      System.out.println("All Clients got spawned.");

      cluster.addClusterListener(this);
      int jmxPort = Integer.valueOf(appConfig.getAttribute(JMX_PORT));
      try {
        jmxc = JMXUtils.getJMXConnector("localhost", jmxPort);
        mbsc = jmxc.getMBeanServerConnection();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
      dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);

      DSOClientMBean[] clients = getDSOClientMBeans();
      while (true) {
        ThreadUtil.reallySleep(500);
        for (int i = 0; i < participantCount; i++) {
          int pendingTransactions = (int) clients[i].getPendingTransactionsCount();
          System.out.println("Client " + i + " Pend Tx Cnt: " + pendingTransactions);
          totalNoOfPendingTransactionsForClient[i] = totalNoOfPendingTransactionsForClient[i] + pendingTransactions;
        }
        if (L1Client.getCurrentNodeID() > MAX_COUNT) break;
      }

      try {
        System.out.println("Coordinator ready to kill");
        finished.await();
        System.out.println("Coordinator started killing");
      } catch (Exception e) {
        throw new AssertionError(e);
      }
      Assert.eval(dsoMBean.getClients().length == TOTAL_L1_PROCESS);

      for (int i = 0; i < TOTAL_L1_PROCESS; i++) {
        if (!clients[i].getNodeID().equals(cluster.getCurrentNode().getId())) {
          clients[i].killClient();
        }
      }

      ThreadUtil.reallySleep(10000);
      Assert.assertEquals(4, totalNoOfDisconnectedClients);
      System.out.println("All clients killed and verified. Test passed");
      Assert.eval(dsoMBean.getClients().length == 1);
      return;
    }

    private DSOClientMBean[] getDSOClientMBeans() {

      ObjectName[] clientObjectNames = dsoMBean.getClients();
      DSOClientMBean[] clients = new DSOClientMBean[clientObjectNames.length];
      for (int i = 0; i < clients.length; i++) {
        clients[i] = MBeanServerInvocationHandler.newProxyInstance(mbsc, clientObjectNames[i], DSOClientMBean.class,
                                                                   false);
      }
      return clients;
    }

    public void nodeJoined(final DsoClusterEvent event) {
      // we are not asserting for connection
      // do nothing
    }

    public void nodeLeft(final DsoClusterEvent event) {
      totalNoOfDisconnectedClients++;
    }

    public void operationsDisabled(final DsoClusterEvent event) {
      // do nothing
    }

    public void operationsEnabled(final DsoClusterEvent event) {
      // do nothing
    }
  }

  /**
   * This class is supposed to make the cpu slow so that we can get a lot of pending transactions
   */
  private class PegCPU implements Runnable {

    public void run() {
      while (true) {
        // check after some time and return when producer has produced everything
        final int MAX_COUNTER = 0x7fffff;
        for (int i = 0; i < MAX_COUNTER; i++) {
          // do nothing, sleep for some time so that even slow machines can spawn the L1 easily
          ThreadUtil.reallySleep(5);
        }
        if (L1Client.getCurrentNodeID() >= MAX_COUNT) return;
      }
    }
  }

  /**
   * Node to be inserted in the shared queue
   */

  private static class MyNode {
    private final int id;

    public MyNode(final int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }
  }
}
