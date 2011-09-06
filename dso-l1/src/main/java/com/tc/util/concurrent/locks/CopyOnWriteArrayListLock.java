/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent.locks;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Used to replace the ReentrantLock 'lock' field for CopyOnWriteArrayList of jdk1.6+
 * 
 * @author hhuynh
 */
public class CopyOnWriteArrayListLock extends ReentrantLock {

  private final CopyOnWriteArrayList cow;

  public CopyOnWriteArrayListLock(CopyOnWriteArrayList cow) {
    this.cow = cow;
  }

  @Override
  public void lock() {
    if (ManagerUtil.isManaged(cow)) {
      ManagerUtil.monitorEnter(cow, Manager.LOCK_TYPE_WRITE);
    }
    super.lock();
  }
  
  @Override
  public void unlock() {
    if (ManagerUtil.isManaged(cow)) {
      ManagerUtil.monitorExit(cow, Manager.LOCK_TYPE_WRITE);
    }
    super.unlock();
  }
}