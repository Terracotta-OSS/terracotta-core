/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.clusterinfo;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class RogueClientTestClient extends ClientBase {

  private static final int          MAX_COUNT = 1000;
  private final ToolkitBarrier      producerFinished;
  private final BlockingQueue<Long> lbqueue;
  private final ToolkitAtomicLong   itemsProcessed;
  private final long                clientId;

  public RogueClientTestClient(final String[] args) {
    super(args);
    this.producerFinished = getClusteringToolkit().getBarrier("finished", getParticipantCount());
    this.itemsProcessed = getClusteringToolkit().getAtomicLong("test long");
    this.lbqueue = getClusteringToolkit().getBlockingQueue("blocking queue", null);
    this.clientId = getClusteringToolkit().getAtomicLong("clientId generator").incrementAndGet();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    startCPUPegger();
    System.out.println("Client number " + clientId + " has been spawned, waiting for others to get spawned");
    waitForAllClients();
    System.out.println("All clients spawned");

    if (clientId % 2 == 0) {
      System.out.println("started as consumer");
      consume();
      producerFinished.await();
      Thread.currentThread().join();
    } else {
      System.out.println("started as producer");
      produce();
      System.out.println("Client " + this.clientId + " finished producing, waiting to be killed");
      producerFinished.await();
      Thread.currentThread().join();
    }
  }

  private void consume() throws InterruptedException {
    while (true) {
      // let consumer be little slow
      ThreadUtil.reallySleep(50);
      Long myNode = lbqueue.poll(100, TimeUnit.MILLISECONDS);

      if (myNode == null) {
        if (this.itemsProcessed.get() >= MAX_COUNT) {
          break;
        }
        continue;
      }

      long id = myNode.longValue();
      if (id % 20 == 0) System.out.println("Clinet " + this.clientId + " consumed node number " + id);
      if (id > MAX_COUNT) break;
    }
  }

  private void produce() throws InterruptedException {
    while (true) {
      long id = this.itemsProcessed.incrementAndGet();
      if (id > MAX_COUNT) break;
      lbqueue.put(id);
      if (id % 20 == 0) System.out.println("Clinet " + this.clientId + " produced node number " + id);
    }
  }

  private void startCPUPegger() {
    // start the thread to peg the cpu
    Thread pegCPUThread = new Thread(new PegCPU(), "CPU Pegger");
    pegCPUThread.start();
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
        if (itemsProcessed.get() >= MAX_COUNT) return;
      }
    }
  }

}
