/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.JMXUtils;
import org.terracotta.toolkit.Toolkit;

import com.tc.management.beans.L2MBeanNames;
import com.tc.stats.api.DSOMBean;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import junit.framework.Assert;

/**
 * Exercise ManagerUtil.waitForAllCurrentTransactionsToComplete() to see if screw up anything
 */
public class WaitForAllCurrentTransactionsToCompleteTestApp extends ClientBase {
  private static final int DEFAULT_NUM_OF_PUT  = 1234;
  private static final int DEFAULT_NUM_OF_LOOP = 5;

  private static final int CAPACITY            = 2000;

  private final Queue      queue;

  private final int        numOfPut;
  private final int        numOfLoop;

  public WaitForAllCurrentTransactionsToCompleteTestApp(String[] args) {
    super(args);

    numOfPut = DEFAULT_NUM_OF_PUT;
    numOfLoop = DEFAULT_NUM_OF_LOOP;
    this.queue = new Queue(CAPACITY, getClusteringToolkit());

    info("***** setting numOfPut=[" + numOfPut + "] numOfLoop=[" + numOfLoop + "]");
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    int index = waitForAllClients();
    for (int i = 0; i < numOfLoop; i++) {
      if (index == 0) {
        doPut();
        Assert.assertEquals(numOfPut + getParticipantCount() - 1, queue.size());
        info("XXX Txns in the system");
        getPendingTransactionsCountForAllClients(toolkit);
      }
      waitForAllClients();
      waitTxnComplete(toolkit);
      waitForAllClients();
      if (index == 0) {
        Thread.sleep(1000);
        info("XXX Txns in the system, after txn complete");
        Assert.assertEquals(0, getPendingTransactionsCountForAllClients(toolkit));
      }
      waitForAllClients();
      if (index != 0) {
        doGet();
        Assert.assertTrue(queue.size() < (getParticipantCount() - 1));
      }
      waitForAllClients();
    }

    waitForAllClients();
  }

  private void waitTxnComplete(Toolkit toolkit) {
    long now = System.currentTimeMillis();
    waitForAllCurrentTransactionsToComplete(toolkit);
    info("XXX waitForAllCurrentTransactionsToComplete took " + (System.currentTimeMillis() - now) + "ms");
  }

  private long getPendingTransactionsCountForAllClients(Toolkit toolkit) {
    MBeanServerConnection mbsc;
    int jmxPort = getTestControlMbean().getGroupsData()[0].getJmxPort(0);
    try {
      JMXConnector jmxc = JMXUtils.getJMXConnector("localhost", jmxPort);
      mbsc = jmxc.getMBeanServerConnection();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    DSOMBean dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
    Map<ObjectName, Long> map = dsoMBean.getAllPendingTransactionsCount();

    long totalPendingTxns = 0;
    for (ObjectName o : map.keySet()) {
      info("Pending Tranaction count for " + o + " size: " + map.get(o));
      String cid = o.getKeyProperty("channelID");
      long l = map.get(o);
      info("XXX Pending Tranaction count for channleID=" + cid + " size: " + l);
      totalPendingTxns += l;
    }
    return totalPendingTxns;
  }

  private void doGet() throws Exception {
    while (true) {
      String o = queue.take();
      if ("STOP".equals(o)) {
        break;
      }
    }
  }

  private void doPut() throws Exception {
    for (int i = 0; i < numOfPut; i++) {
      // System.out.println("Putting " + i);
      queue.put("RANDOM STRING" + i);
      Assert.assertTrue(queue.size() <= CAPACITY);
    }
    int numOfGet = getParticipantCount() - 1;
    for (int i = 0; i < numOfGet; i++) {
      queue.put("STOP");
    }
  }

  private static class Queue {

    private final BlockingQueue<String> list;

    public Queue(int capacity, Toolkit clusteringToolkit) {
      this.list = clusteringToolkit.getBlockingQueue("test queue", capacity, null);
    }

    public String take() throws InterruptedException {
      return this.list.take();
    }

    public void put(String o) {
      this.list.add(o);
    }

    public int size() {
      return this.list.size();
    }
  }

  private void info(String string) {
    System.out.println(string);
  }

}
