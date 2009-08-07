/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package java.util.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FutureTaskTC implements Future, Runnable {
  private final Sync sync;

  public FutureTaskTC(Callable callable) {
    if (callable == null) throw new NullPointerException();
    sync = new Sync(callable);
  }

  public FutureTaskTC(Runnable runnable, Object result) {
    sync = new Sync(Executors.callable(runnable, result));
  }

  public boolean isCancelled() {
    return sync.innerIsCancelled();
  }

  public boolean isDone() {
    return sync.innerIsDone();
  }

  public boolean cancel(boolean mayInterruptIfRunning) {
    return sync.innerCancel(mayInterruptIfRunning);
  }

  public Object get() throws InterruptedException, ExecutionException {
    return sync.innerGet();
  }

  public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return sync.innerGet(unit.toNanos(timeout));
  }

  protected void done() {
    //
  }

  protected void set(Object v) {
    sync.innerSet(v);
  }

  protected void setException(Throwable t) {
    sync.innerSetException(t);
  }

  public void run() {
    sync.innerRun();
  }

  protected boolean runAndReset() {
    return sync.innerRunAndReset();
  }

  private final class Sync {
    private static final int          RUNNING   = 1;
    private static final int          RAN       = 2;
    private static final int          CANCELLED = 4;

    private final Callable            callable;
    private Object                    result;
    private Throwable                 exception;

    private int                       state;
    private final ReentrantLock       lock;
    private final Condition           ran;

    private transient volatile Thread runner;
    private Object                    proxyRunner;

    Sync(Callable callable) {
      this.callable = callable;
      lock = new ReentrantLock();
      ran = lock.newCondition();
    }

    @SuppressWarnings("unused")
    Sync() {
      callable = null;
      lock = null;
      ran = null;
    }

    private boolean ranOrCancelled(int stateArg) {
      return (stateArg & (RAN | CANCELLED)) != 0;
    }

    private int tryAcquireShared() {
      return innerIsDone() ? 1 : -1;
    }

    protected boolean tryReleaseShared(int ignore) {
      managedTryReleaseShared();
      return true;
    }

    private boolean managedTryReleaseShared() {
      runner = null;
      proxyRunner = null;
      ran.signalAll();
      return true;
    }

    private final void setState(int state) {
      this.state = state;
    }

    private final int getSynchronizedState() {
      lock.lock();
      try {
        return state;
      } finally {
        lock.unlock();
      }
    }

    private final boolean compareAndSetState(int expected, int newValue) {
      lock.lock();
      try {
        int s = state;
        if (s == expected) {
          setState(newValue);
          return true;
        }
      } finally {
        lock.unlock();
      }
      return false;
    }

    boolean innerIsCancelled() {
      return getSynchronizedState() == CANCELLED;
    }

    boolean innerIsDone() {
      lock.lock();
      try {
        boolean ranOrCancelled = ranOrCancelled(state);
        return ranOrCancelled && proxyRunner == null;
      } finally {
        lock.unlock();
      }
    }

    Object innerGet() throws InterruptedException, ExecutionException {
      lock.lock();
      try {
        while (tryAcquireShared() < 0) {
          ran.await();
        }
      } finally {
        lock.unlock();
      }

      if (getSynchronizedState() == CANCELLED) throw new CancellationException();
      if (exception != null) throw new ExecutionException(exception);
      return result;
    }

    Object innerGet(long nanosTimeout) throws InterruptedException, ExecutionException, TimeoutException {
      lock.lock();
      try {
        long startTime = System.nanoTime();
        while ((tryAcquireShared() < 0) && (nanosTimeout > 0)) {
          ran.await(nanosTimeout, TimeUnit.NANOSECONDS);
          long endTime = System.nanoTime();
          nanosTimeout -= (endTime - startTime);
        }
        if (tryAcquireShared() < 0) { throw new TimeoutException(); }
      } finally {
        lock.unlock();
      }

      if (getSynchronizedState() == CANCELLED) throw new CancellationException();
      if (exception != null) throw new ExecutionException(exception);
      return result;
    }

    void innerSet(Object v) {
      lock.lock();
      try {
        managedInnerSet(v);
      } finally {
        lock.unlock();
      }
    }

    private void managedInnerSet(Object v) {
      int s = state;
      if (ranOrCancelled(s)) return;
      setState(RAN);
      result = v;
      managedTryReleaseShared();
      done();
    }

    void innerSetException(Throwable t) {
      lock.lock();
      try {
        int s = state;
        if (ranOrCancelled(s)) return;
        setState(RAN);
        exception = t;
        result = null;
        managedTryReleaseShared();
        done();
      } finally {
        lock.unlock();
      }
    }

    private void managedInnerCancel() {
      Thread r = null;
      lock.lock();
      try {
        r = runner;
      } finally {
        lock.unlock();
      }
      if (r != null) {
        r.interrupt();
      }
    }

    boolean innerCancel(boolean mayInterruptIfRunning) {
      lock.lock();
      try {
        int s = state;
        if (ranOrCancelled(s)) return false;
        setState(CANCELLED);
      } finally {
        lock.unlock();
      }
      lock.lock();
      try {
        if (mayInterruptIfRunning) {
          managedInnerCancel();
        }
        tryReleaseShared(0);
      } finally {
        lock.unlock();
      }
      done();
      return true;
    }

    void innerRun() {
      if (!compareAndSetState(0, RUNNING)) return;

      try {
        boolean isRunning = false;
        lock.lock();
        try {
          isRunning = state == RUNNING;
          if (isRunning) {
            runner = Thread.currentThread();
            proxyRunner = runner.toString();
          }
        } finally {
          lock.unlock();
        }
        if (isRunning) {
          Object o = callable.call();
          lock.lock();
          try {
            managedInnerSet(o);
          } finally {
            lock.unlock();
          }
        } else {
          lock.lock();
          try {
            managedTryReleaseShared();
          } finally {
            lock.unlock();
          }
        }
      } catch (Throwable ex) {
        innerSetException(ex);
      }
    }

    boolean innerRunAndReset() {
      if (!compareAndSetState(0, RUNNING)) return false;
      try {
        runner = Thread.currentThread();
        callable.call();
        runner = null;
        return compareAndSetState(RUNNING, 0);
      } catch (Throwable ex) {
        innerSetException(ex);
        return false;
      }
    }
  }
}
