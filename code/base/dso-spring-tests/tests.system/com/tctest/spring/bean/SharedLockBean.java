/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import com.tc.aspectwerkz.proxy.Uuid;

import java.util.ArrayList;
import java.util.List;

/**
 *  This class is testing shared lock behavior
 *  1. Locking on shared object applies to the whole cluster
 *  2. Trying to modify a shared objec outside a shared lock should raise exception
 *  3. Shared value will not be propagated until execution thread exits the monitor
 */
public class SharedLockBean implements Runnable, ISharedLock {
  // shared variables
  private List sharedVar = new ArrayList();
  private Object sharedLock = new Object();
//  private Object mutex = new Object();
  
  // variables not shared
  private transient List unsharedVar = new ArrayList();
  private transient long localID = System.identityHashCode(this) + Uuid.newUuid();
  private transient volatile int stepId = 0;
  
  public void start() {
    Thread th = new Thread(this);
    th.setDaemon(true);
    th.start();
  }
  
  public void moveToStep(int step) {
    stepId = step;
    try { 
      // give enough time for the blocking thread to take action 
      Thread.sleep(1000L);
    } catch (Exception e) {e.printStackTrace();}

// XXX should rewrite that with wait/notify, for now just increased timeout
//    synchronized(this.mutex) {
//      this.mutex.notifyAll();
//    }
  }
   
  public void run() {
    holdOnStep(10);           // below is step 10
    try {
      sharedVar.add("ckpoint1-" + localID);
    } catch (Exception e) {
      // except an exception since not synchronized
      unsharedVar.add("ckpoint1-" + localID);
    }
    holdOnStep(20);           // below is step 20
    synchronized(this.sharedLock) {
      sharedVar.add("ckpoint2-" + localID);
      holdOnStep(30);         // below is step 30 
    } // shared value propagation point
    holdOnStep(40);           // below is step 40 and return
  }
  
  private void holdOnStep(int step) {
    while (stepId < step) {
      try {
        Thread.sleep(100L);
      } catch (Exception e) {e.printStackTrace();}
//      synchronized(this.mutex) {
//        try {
//          this.mutex.wait(100L);
//          System.err.println("### SharedLockBean.holdOnStep() "+stepId);
//        } catch (InterruptedException e) {
//          // ignore
//          System.err.println("### SharedLockBean.holdOnStep() INTERRUPTED "+stepId);
//        }
//      }
    }
  }

  // XXX shouldn't this be locked?
  public List getSharedVar() {
    return sharedVar;
  }

  public List gethUnSharedVar() {
    return unsharedVar;
  }

  public long getLocalID() {
    return localID;
  }
}
