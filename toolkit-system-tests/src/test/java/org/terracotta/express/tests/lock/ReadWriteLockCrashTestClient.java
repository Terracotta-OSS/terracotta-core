/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

import junit.framework.Assert;

public class ReadWriteLockCrashTestClient extends ClientBase {

  private List                dataList;
  private ReadWriteLock       readWriteLock;
  private ToolkitAtomicLong counter;
  private static final int    noOfElements = 500;

  public ReadWriteLockCrashTestClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) throws Exception {
    getBarrierForAllClients().await();
    dataList = toolkit.getList("dataList", null);
    readWriteLock = toolkit.getReadWriteLock("ReadWriteLock");
    counter = toolkit.getAtomicLong("counter");

    populate();
    getBarrierForAllClients().await();
    validate();

  }

  private void validate() throws Exception {
    readWriteLock.readLock().lock();
    try {
      getBarrierForAllClients().await();
      Long current = 1L;
      for (Object data : dataList) {
        String dataString = (String) data;
        Assert.assertEquals(getDataString(current), dataString);
        current++;
      }

    } finally {
      readWriteLock.readLock().unlock();
    }

  }

  private void info(Object msg) {
    System.out.println(this + ": " + msg);
  }

  public synchronized void populate() {

    info("Populating dataList with " + noOfElements + " items...");
    for (int i = 0; i < noOfElements; i++) {
      readWriteLock.writeLock().lock();
      try {
        dataList.add(getNextString());
      } finally {
        readWriteLock.writeLock().unlock();
      }
    }
    info("Done populating dataList.");
  }

  private String getNextString() {
    return getDataString(counter.incrementAndGet());
  }

  private String getDataString(Long value) {
    return "Data-" + value;
  }

}
