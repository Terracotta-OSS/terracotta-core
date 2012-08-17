/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

public class MutateDGCValidateTestMutator extends ClientBase {
  static final int            OBJECT_COUNT = 5000;
  private final List<Integer> rootList;
  private final ReadWriteLock lock;

  public MutateDGCValidateTestMutator(String[] args) {
    super(args);
    this.rootList = getClusteringToolkit().getList("testList", null);
    this.lock = getClusteringToolkit().getReadWriteLock("testLock");
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    lock.writeLock().lock();
    try {
      for (int i = 0; i < OBJECT_COUNT; i++) {
        this.rootList.add(i);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }
}
