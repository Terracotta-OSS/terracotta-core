/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;
import com.tc.object.locks.ClientLockImpl.LockAcquireResult;
import com.tc.util.Assert;
import com.tc.util.SinglyLinkedList;

import java.util.Stack;
import java.util.concurrent.locks.LockSupport;

abstract class LockStateNode implements SinglyLinkedList.LinkedNode<LockStateNode> {
  private final ThreadID owner;
  
  private LockStateNode  next;

  LockStateNode(ThreadID owner) {
    this.owner = owner;
    this.next = null;
  }
  
  /**
   * @throws InterruptedException can be thrown by certain subclasses
   */
  void park() throws InterruptedException {
    throw new AssertionError();
  }

  /**
   * Parks for at most <code>timeout</code> milliseconds.
   * <p>
   * <code>LockStateNode.park(0);</code> does *not* sleep indefinitely.
   * 
   * @throws InterruptedException can be thrown by certain subclasses
   */
  void park(long timeout) throws InterruptedException {
    throw new AssertionError();
  }

  void unpark() {
    throw new AssertionError();
  }
  
  ThreadID getOwner() {
    return owner;
  }
  
  LockAcquireResult allowsHold(LockHold newHold) {
    return LockAcquireResult.UNKNOWN;
  }
  
  abstract ClientServerExchangeLockContext toContext(LockID lock, ClientID node);
  
  public LockStateNode getNext() {
    return next;
  }

  public LockStateNode setNext(LockStateNode newNext) {
    LockStateNode old = next;
    next = newNext;
    return old;
  }
  
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof LockStateNode) {
      return (owner.equals(((LockStateNode) o).owner));
    } else {
      return false;
    }
  }
  
  public String toString() {
    return getClass().getSimpleName() + " : " + owner;
  }
  
  static class LockHold extends LockStateNode {
    private final LockLevel level;
    
    LockHold(ThreadID owner, LockLevel level) {
      super(owner);
      this.level = level;
    }
    
    LockLevel getLockLevel() {
      return level;
    }
    
    @Override
    LockAcquireResult allowsHold(LockHold newHold) {
      if (getOwner().equals(newHold.getOwner())) {
        if (this.getLockLevel().isWrite()) {
          return LockAcquireResult.SUCCESS;
        }
        if (newHold.getLockLevel().isRead()) {
          return LockAcquireResult.SHARED_SUCCESS;
        }
      } else {
        if (this.getLockLevel().isWrite() || newHold.getLockLevel().isWrite()) {
          return LockAcquireResult.FAILURE;
        }
      }
      
      return LockAcquireResult.UNKNOWN;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof LockHold) {
        return super.equals(o) && level.equals(((LockHold) o).level);
      } else {
        return false;
      }
    }
    
    @Override
    public int hashCode() {
      return (5 * super.hashCode()) ^ (7 * level.hashCode());
    }

    @Override
    public String toString() {
      return super.toString() + " : " + level;
    }

    @Override
    ClientServerExchangeLockContext toContext(LockID lock, ClientID node) {
      switch (ServerLockLevel.fromClientLockLevel(getLockLevel())) {
        case READ: return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.HOLDER_READ);
        case WRITE: return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.HOLDER_WRITE);
      }
      throw new AssertionError();
    }
  }
  
  static class PendingLockHold extends LockStateNode {
    private final LockLevel  level;
    private final Thread     javaThread;
    private final long       waitTime;
    private volatile boolean responded = false;
    private volatile boolean awarded   = false;
    private volatile boolean delegates = true;
    
    PendingLockHold(ThreadID owner, LockLevel level, long timeout) {
      super(owner);
      this.javaThread = Thread.currentThread();
      this.level = level;
      this.waitTime = timeout;
    }
    
    LockLevel getLockLevel() {
      return level;
    }
    
    Thread getJavaThread() {
      return javaThread;
    }
    
    long getTimeout() {
      return waitTime;
    }
    
    void park() {
      Assert.assertEquals(getJavaThread(), Thread.currentThread());
      LockSupport.park();
    }
    
    void park(long timeout) {
      Assert.assertEquals(getJavaThread(), Thread.currentThread());
      LockSupport.parkNanos(timeout * 1000000L);
    }
    
    void unpark() {
      LockSupport.unpark(javaThread);
    }

    boolean canDelegate() {
      return delegates;
    }

    void delegated() {
      delegates = false;
    }
    
    void refused() {
      awarded = false;
      responded = true;
    }

    void awarded() {
      awarded = true;
      responded = true;
    }
    
    boolean isRefused() {
      return responded && !awarded;
    }

    boolean isAwarded() {
      return responded && awarded;
    }
    
    LockAcquireResult allowsHold(LockHold newHold) {
      if (getOwner().equals(newHold.getOwner()) && getLockLevel().equals(newHold.getLockLevel())) {
        if (responded) {
          if (awarded) {
            return LockAcquireResult.SUCCESS;
          } else {
            return LockAcquireResult.FAILURE;
          }
        }
      }
      return LockAcquireResult.UNKNOWN;
    }
    
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof PendingLockHold) {
        return super.equals(o) && level.equals(((PendingLockHold) o).level);
      } else {
        return false;
      }
    }
    
    public int hashCode() {
      return (5 * super.hashCode()) ^ (7 * level.hashCode());
    }
        
    public String toString() {
      return super.toString() + " : " + level;
    }
    
    @Override
    ClientServerExchangeLockContext toContext(LockID lock, ClientID node) {
      switch (ServerLockLevel.fromClientLockLevel(getLockLevel())) {
        case READ:
          if (getTimeout() < 0) {
            return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.PENDING_READ);
          } else {
            return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.TRY_PENDING_READ, getTimeout());
          }
        case WRITE:
          if (getTimeout() < 0) {
            return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.PENDING_WRITE);
          } else {
            return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.TRY_PENDING_WRITE, getTimeout());
          }
      }
      throw new AssertionError();
    }
  }
  
  static class MonitorBasedPendingLockHold extends PendingLockHold {

    private final Object waitObject;
    private boolean      unparked = false;
    
    MonitorBasedPendingLockHold(ThreadID owner, LockLevel level, Object waitObject, long timeout) {
      super(owner, level, timeout);
      if (waitObject == null) {
        this.waitObject = this;
      } else {
        this.waitObject = waitObject;
      }
    }
    
    @Override
    void park() {
      synchronized (waitObject) {
        while (!unparked) {
          try {
            waitObject.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            unparked = true;
          }
        }
        unparked = false;
      }
    }

    /**
     * MonitorBasedQueuedLockAcquires are only used to reacquire locks after waiting
     *  - they should always park indefinitely
     */
    @Override
    void park(long timeout) {
      throw new AssertionError();
    }
    
    @Override
    void unpark() {
      synchronized (waitObject) {
        unparked = true;
        waitObject.notifyAll();
      }
    }
    
    @Override
    boolean canDelegate() {
      return false;
    }
  }
  
  static class LockWaiter extends LockStateNode {
    
    private final Object                 waitObject;
    private final long                   waitTime;
    private final Stack<PendingLockHold> reacquires;
    
    private volatile boolean             notified;
    
    LockWaiter(ThreadID owner, Object waitObject, Stack<LockHold> holds, long timeout) {
      super(owner);
      if (waitObject == null) {
        this.waitObject = this;
      } else {
        this.waitObject = waitObject;
      }
      
      this.reacquires = new Stack<PendingLockHold>();
      for (LockHold hold : holds) {
        reacquires.add(new MonitorBasedPendingLockHold(owner, hold.getLockLevel(), this.waitObject, ClientLockImpl.BLOCKING_LOCK));
      }
      
      this.waitTime = timeout;
    }
    
    long getTimeout() {
      return waitTime;
    }

    Stack<PendingLockHold> getReacquires() {
      return reacquires;
    }
    
    void park() throws InterruptedException {
      synchronized (waitObject) {
        while (!notified) {
          waitObject.wait();
        }
      }
    }

    void park(long timeout) throws InterruptedException {
      long lastTime = System.currentTimeMillis();
      synchronized (waitObject) {
        while (!notified && timeout > 0) {
          waitObject.wait(timeout);
          long now = System.currentTimeMillis();
          timeout -= now - lastTime;
        }
      }
    }
    
    void unpark() {
      synchronized (waitObject) {
        notified = true;
        waitObject.notifyAll();
      }
    }
    
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof LockWaiter) {
        return super.equals(o);
      } else {
        return false;
      }
    }
    
    public int hashCode() {
      return super.hashCode();
    }
    
    @Override
    ClientServerExchangeLockContext toContext(LockID lock, ClientID node) {
      return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.WAITER, getTimeout());
    }
  }  
}