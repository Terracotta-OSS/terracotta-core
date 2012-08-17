/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.gcrunner.GCRunner;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

import junit.framework.Assert;

public class MutateDGCValidateTestValidator extends ClientBase {
  private final List<Integer> rootList;
  private final ReadWriteLock lock;

  public MutateDGCValidateTestValidator(String[] args) {
    super(args);
    this.rootList = getClusteringToolkit().getList("testList", null);
    this.lock = getClusteringToolkit().getReadWriteLock("testLock");
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    performDGC();

    Thread.sleep(10000);

    getTestControlMbean().crashActiveServer(0);
    Thread.sleep(10000);
    getTestControlMbean().reastartLastCrashedServer(0);

    validate();
    System.out.println("Test finished.");
  }

  private void performDGC() {
    GCRunner runner = new GCRunner("localhost", getTestControlMbean().getGroupsData()[0].getJmxPort(0));
    try {
      runner.runGC();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void validate() {
    lock.readLock().lock();
    try {
      Assert.assertEquals(MutateDGCValidateTestMutator.OBJECT_COUNT, rootList.size());
      for (int i = 0; i < MutateDGCValidateTestMutator.OBJECT_COUNT; i++) {
        Assert.assertEquals(i, (rootList.get(i)).intValue());
      }
    } finally {
      lock.readLock().unlock();
    }
  }

}
