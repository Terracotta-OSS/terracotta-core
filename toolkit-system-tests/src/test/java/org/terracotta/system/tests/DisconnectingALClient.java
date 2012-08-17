/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

public class DisconnectingALClient extends ClientBase {

  private final long timeout;

  public DisconnectingALClient(String[] args) {
    super(args);
    this.timeout = Integer.getInteger("shortDuration");
  }

  @Override
  protected void test(Toolkit toolkit) {
    long total = 0;
    List list = toolkit.getList("testList", null);
    ReadWriteLock lock = toolkit.getReadWriteLock("testLock");
    while (keepGoing()) {
      total = 0;
      lock.writeLock().lock();
      try {
        for (Object element : list) {
          String str = (String) element;
          // Pretend like we are using this string.
          str.hashCode();
        }
      } finally {
        lock.writeLock().unlock();
      }
    }
    System.err.println("DisconnectingALClient " + " stopping with total " + total);
  }

  private boolean keepGoing() {
    return timeout == 0L || System.currentTimeMillis() < timeout;
  }

}
