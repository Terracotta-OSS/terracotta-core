/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

import junit.framework.Assert;

public class TxReorderingClient extends ClientBase {

  public TxReorderingClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) throws Exception {
    int id = getBarrierForAllClients().await();
    ReadWriteLock rrwl = toolkit.getReadWriteLock("rrwl");
    List list = toolkit.getList("list", null);
    if (id == 0) {
      System.out.println("Popluating Data");
      rrwl.writeLock().lock();
      list.add("data1");
      list.add("data2");
      rrwl.writeLock().unlock();
    }

    waitForAllClients();
    if (id == 1) {
      rrwl.readLock().lock();
      System.out.println("Reading 1st time: ");
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(list.get(0), "data1");
      Assert.assertEquals(list.get(1), "data2");
      rrwl.readLock().unlock();
    }

    waitForAllClients();
    if (id == 0) {
      rrwl.writeLock().lock();
      Object data = list.get(0);
      list.remove(data);
      list.add("data3");
      System.out.println(list.get(0));
      rrwl.writeLock().unlock();
    }

    waitForAllClients();
    if (id == 1) {
      rrwl.readLock().lock();
      System.out.println("Reading 2nd time: ");
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(list.get(0), "data2");
      Assert.assertEquals(list.get(1), "data3");
      rrwl.readLock().unlock();
    }
  }

}
