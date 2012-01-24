/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.builtin;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

public class Lock {

  public boolean tryWriteLock() {
    return ManagerUtil.tryMonitorEnter(this, Manager.LOCK_TYPE_WRITE);
  }

  public boolean tryReadLock() {
    return ManagerUtil.tryMonitorEnter(this, Manager.LOCK_TYPE_READ);
  }

  public void writeLock() {
    ManagerUtil.monitorEnter(this, Manager.LOCK_TYPE_WRITE);
  }

  public void writeUnlock() {
    ManagerUtil.monitorExit(this, Manager.LOCK_TYPE_WRITE);
  }

  public void readLock() {
    ManagerUtil.monitorEnter(this, Manager.LOCK_TYPE_READ);
  }

  public void readUnlock() {
    ManagerUtil.monitorExit(this, Manager.LOCK_TYPE_READ);
  }

  public void doWait() throws InterruptedException {
    ManagerUtil.objectWait(this);
  }

  public void doNotify() {
    ManagerUtil.objectNotify(this);
  }

  public void doNotifyAll() {
    ManagerUtil.objectNotifyAll(this);
  }

  public void doWait(long millis) throws InterruptedException {
    ManagerUtil.objectWait(this, millis);
  }

}
