/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;


public class LockEventMonitor implements LockEventListener {

  public static final int DEFAULT_WAITER_COUNT = -1;
  
  private LinkedQueue notifyAddPendingCalls = new LinkedQueue();
  private LinkedQueue notifyAwardCalls = new LinkedQueue();
  private LinkedQueue notifyRevokeCalls = new LinkedQueue();
  
  public void notifyAddPending(int waiterCount, LockAwardContext ctxt) {
    enqueue(notifyAddPendingCalls, new CallContext(waiterCount, ctxt));
  }

  public void notifyAward(int waiterCount, LockAwardContext ctxt) {
    enqueue(notifyAwardCalls, new CallContext(waiterCount, ctxt));
  }
  
  public void notifyRevoke(LockAwardContext ctxt) {
    enqueue(notifyRevokeCalls, new CallContext(DEFAULT_WAITER_COUNT, ctxt));
  }

  public CallContext waitForNotifyAddPending(long timeout) throws Exception {
    return dequeue(notifyAddPendingCalls, timeout);
  }

  public CallContext waitForNotifyAward(long timeout) throws Exception {
    return dequeue(notifyAwardCalls, timeout);
  }

  public CallContext waitForNotifyRevoke(long timeout) throws Exception {
    return dequeue(notifyRevokeCalls, timeout);
  }

  public void reset() throws Exception {
    drainQueue(notifyAddPendingCalls);
    drainQueue(notifyAwardCalls);
    drainQueue(notifyRevokeCalls);
  }
  
  private void enqueue(LinkedQueue queue, Object o) {
    try {
      queue.put(o);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  private CallContext dequeue(LinkedQueue queue, long timeout) throws Exception {
    return (CallContext) queue.poll(timeout);
  }
  
  private void drainQueue(LinkedQueue queue) throws Exception {
    while (! queue.isEmpty()) {
      queue.poll(0);
    }
  }
  
  public class CallContext {
    public final int waiterCount;
    public final LockAwardContext ctxt;
    
    public CallContext(int waiterCount, LockAwardContext ctxt) {
      this.waiterCount = waiterCount;
      this.ctxt = ctxt;
    }
  }

}