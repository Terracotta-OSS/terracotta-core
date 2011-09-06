/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;
import com.tc.object.locks.ClientLockImpl.LockAcquireResult;
import com.tc.util.Assert;
import com.tc.util.FindbugsSuppressWarnings;
import com.tc.util.SinglyLinkedList;

import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

abstract class LockStateNode implements SinglyLinkedList.LinkedNode<LockStateNode> {

  private static final ExecutorService UNPARK_HANDLER = Executors.newCachedThreadPool(new ThreadFactory() {
                                                        public Thread newThread(Runnable r) {
                                                          Thread t = new Thread(r, "Unpark Handler Thread");
                                                          t.setDaemon(true);
                                                          return t;
                                                        }
                                                      });

  private final ThreadID               owner;

  private LockStateNode                next;

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

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof LockStateNode) {
      return (owner.equals(((LockStateNode) o).owner));
    } else {
      return false;
    }
  }

  @Override
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
        if (this.getLockLevel().isWrite()) { return LockAcquireResult.SUCCESS; }
        if (newHold.getLockLevel().isRead()) { return LockAcquireResult.SHARED_SUCCESS; }
      } else {
        if (this.getLockLevel().isWrite() || newHold.getLockLevel().isWrite()) { return LockAcquireResult.FAILURE; }
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
        case READ:
          return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.HOLDER_READ);
        case WRITE:
          return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.HOLDER_WRITE);
      }
      throw new AssertionError();
    }
  }

  static class PendingLockHold extends LockStateNode {
    private final LockLevel  level;
    private final Thread     javaThread;
    final Semaphore          permit           = new Semaphore(0);

    private volatile boolean delegates        = true;

    volatile boolean         responded        = false;
    volatile boolean         awarded          = false;

    private volatile String  delegationMethod = "Not Delegated";

    PendingLockHold(ThreadID owner, LockLevel level) {
      super(owner);
      this.javaThread = Thread.currentThread();
      this.level = level;
    }

    LockLevel getLockLevel() {
      return level;
    }

    Thread getJavaThread() {
      return javaThread;
    }

    @Override
    void park() {
      Assert.assertEquals(getJavaThread(), Thread.currentThread());
      try {
        permit.acquire();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override
    void unpark() {
      permit.release();
    }

    boolean canDelegate() {
      return delegates;
    }

    void delegated(String method) {
      if (delegates) {
        delegationMethod = method;
      }
      delegates = false;
    }

    void refused() {
      // no-op
    }

    void awarded() {
      awarded = true;
      responded = true;
    }

    boolean isRefused() {
      return false;
    }

    boolean isAwarded() {
      return responded && awarded;
    }

    @Override
    LockAcquireResult allowsHold(LockHold newHold) {
      if (getOwner().equals(newHold.getOwner()) && getLockLevel().equals(newHold.getLockLevel())) {
        if (isAwarded()) { return LockAcquireResult.SUCCESS; }
        if (isRefused()) { return LockAcquireResult.FAILURE; }
      }
      return LockAcquireResult.UNKNOWN;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof PendingLockHold) {
        return super.equals(o) && level.equals(((PendingLockHold) o).level);
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
      return super.toString() + " : " + getLockLevel() + " : delegated=" + !canDelegate() + ", awarded=" + isAwarded()
             + ", refused=" + isRefused() + ", " + delegationMethod;
    }

    @Override
    ClientServerExchangeLockContext toContext(LockID lock, ClientID node) {
      if (isAwarded()) {
        switch (ServerLockLevel.fromClientLockLevel(getLockLevel())) {
          case READ:
            return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.HOLDER_READ);
          case WRITE:
            return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.HOLDER_WRITE);
        }
      } else {
        switch (ServerLockLevel.fromClientLockLevel(getLockLevel())) {
          case READ:
            return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.PENDING_READ);
          case WRITE:
            return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.PENDING_WRITE);
        }
      }
      throw new AssertionError();
    }

    public void allowDelegation() {
      delegates = true;
    }
  }

  static class PendingTryLockHold extends PendingLockHold {

    private final long waitTime;

    PendingTryLockHold(ThreadID owner, LockLevel level, long timeout) {
      super(owner, level);
      this.waitTime = timeout;
    }

    long getTimeout() {
      return waitTime;
    }

    @Override
    boolean isRefused() {
      return responded && !awarded;
    }

    @Override
    void refused() {
      awarded = false;
      responded = true;
    }

    /*
     * Return value of tryAcquire is not relevant... i just want a timed escape from the acquire.
     */
    @FindbugsSuppressWarnings("RV_RETURN_VALUE_IGNORED")
    @Override
    void park(long timeout) {
      Assert.assertEquals(getJavaThread(), Thread.currentThread());
      try {
        permit.tryAcquire(timeout, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override
    ClientServerExchangeLockContext toContext(LockID lock, ClientID node) {
      if (isAwarded()) {
        switch (ServerLockLevel.fromClientLockLevel(getLockLevel())) {
          case READ:
            return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.HOLDER_READ);
          case WRITE:
            return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.HOLDER_WRITE);
        }
      } else {
        switch (ServerLockLevel.fromClientLockLevel(getLockLevel())) {
          case READ:
            return new ClientServerExchangeLockContext(lock, node, getOwner(),
                                                       ServerLockContext.State.TRY_PENDING_READ, getTimeout());
          case WRITE:
            return new ClientServerExchangeLockContext(lock, node, getOwner(),
                                                       ServerLockContext.State.TRY_PENDING_WRITE, getTimeout());
        }
      }
      throw new AssertionError();
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof PendingTryLockHold) {
        return super.equals(o) && waitTime == ((PendingTryLockHold) o).waitTime;
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return (5 * super.hashCode()) ^ (7 * (int) waitTime) ^ (11 * (int) (waitTime >>> Integer.SIZE));
    }

    @Override
    public String toString() {
      return super.toString() + ", timeout=" + getTimeout();
    }
  }

  static class MonitorBasedPendingLockHold extends PendingLockHold {

    private final Object waitObject;
    private boolean      unparked = false;

    MonitorBasedPendingLockHold(ThreadID owner, LockLevel level, Object waitObject) {
      super(owner, level);
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

    @Override
    void unpark() {
      Runnable unparker = new Runnable() {
        public void run() {
          synchronized (waitObject) {
            unparked = true;
            waitObject.notifyAll();
          }
        }
      };
      try {
        UNPARK_HANDLER.execute(unparker);
      } catch (RejectedExecutionException e) {
        new Thread(unparker, "Temporary Unparker Thread [" + toString() + "]").start();
      }
    }

    @Override
    boolean canDelegate() {
      return false;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof MonitorBasedPendingLockHold) {
        return super.equals(o);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

  }

  static class LockWaiter extends LockStateNode {

    private final Object                 waitObject;
    private final long                   waitTime;
    private final Stack<PendingLockHold> reacquires;
    private boolean                      notified = false;

    LockWaiter(ThreadID owner, Object waitObject, Stack<LockHold> holds, long timeout) {
      super(owner);
      if (waitObject == null) {
        this.waitObject = this;
      } else {
        this.waitObject = waitObject;
      }

      this.reacquires = new Stack<PendingLockHold>();
      for (LockHold hold : holds) {
        reacquires.add(new MonitorBasedPendingLockHold(owner, hold.getLockLevel(), this.waitObject));
      }

      this.waitTime = timeout;
    }

    long getTimeout() {
      return waitTime;
    }

    Stack<PendingLockHold> getReacquires() {
      return reacquires;
    }

    /*
     * There is no loop for this wait in Terracotta code. The loop will be in the user code that calls the lock manager.
     */
    @FindbugsSuppressWarnings("WA_NOT_IN_LOOP")
    @Override
    void park() throws InterruptedException {
      synchronized (waitObject) {
        if (!notified) {
          waitObject.wait();
        }
      }
    }

    @Override
    void park(long timeout) throws InterruptedException {
      synchronized (waitObject) {
        if (!notified) {
          waitObject.wait(timeout);
        }
      }
    }

    @Override
    void unpark() {
      // this is a slight hack to avoiding blocking the stage thread
      Runnable unparker = new Runnable() {
        public void run() {
          synchronized (waitObject) {
            notified = true;
            waitObject.notifyAll();
          }
        }
      };
      try {
        UNPARK_HANDLER.execute(unparker);
      } catch (RejectedExecutionException e) {
        new Thread(unparker, "Temporary Unpark Thread [" + toString() + "]").start();
      }
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof LockWaiter) {
        return super.equals(o);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Override
    ClientServerExchangeLockContext toContext(LockID lock, ClientID node) {
      return new ClientServerExchangeLockContext(lock, node, getOwner(), ServerLockContext.State.WAITER, getTimeout());
    }
  }

  public static void shutdown() {
    UNPARK_HANDLER.shutdown();
  }
}