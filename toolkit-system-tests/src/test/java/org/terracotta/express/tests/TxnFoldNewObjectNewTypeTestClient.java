/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import junit.framework.Assert;

public class TxnFoldNewObjectNewTypeTestClient extends ClientBase {

  private final ToolkitBarrier            barrier;
  private final List<String>              badList;
  private final Map<String, Serializable> root;
  private final ReadWriteLock             lock;

  public TxnFoldNewObjectNewTypeTestClient(String[] args) {
    super(args);
    barrier = getClusteringToolkit().getBarrier("testBarrier", getParticipantCount());
    this.badList = getClusteringToolkit().getList("badList", null);
    this.root = getClusteringToolkit().getMap("testMap", null, null);
    this.lock = getClusteringToolkit().getReadWriteLock("lock");
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    int index = barrier.await();

    if (index == 0) {
      produceProblematicFold();
    }

    barrier.await();

    this.lock.readLock().lock();
    String bad = badList.iterator().next();
    Assert.assertEquals("random string", bad);
    this.lock.readLock().unlock();
  }

  private void produceProblematicFold() {
    // get the local txn buffer full-ish so that folding might occur
    for (int i = 0; i < 5000; i++) {
      root.put(String.valueOf(i), new MyClass(i));
    }

    String bad = "random string";
    this.lock.writeLock().lock();
    badList.add(bad);
    this.lock.writeLock().unlock();

    // use a common lock and mutate the field of a "new" instance for a "new" type
    // This mutation will likely be folded into the whole DNA for the Bad instance in the previous transaction
    this.lock.writeLock().lock();
    bad.replace("random", "new Random");
    this.lock.writeLock().unlock();
  }

  public static class MyClass implements Serializable {
    private int value;

    public int getValue() {
      return value;
    }

    public void setValue(int value) {
      this.value = value;
    }

    public MyClass(int value) {
      this.value = value;
    }
  }

}