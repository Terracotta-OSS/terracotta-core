package com.tctest.builtin;

import com.tctest.BuiltinBarrier;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeoutException;

/**
 * A cyclic barrier is a reasonable choice for a barrier in contexts involving a fixed sized group of threads that must
 * occasionally wait for each other. (A Rendezvous better handles applications in which any number of threads meet,
 * n-at-a-time.)
 * <p>
 * CyclicBarriers use an all-or-none breakage model for failed synchronization attempts: If threads leave a barrier
 * point prematurely because of timeout or interruption, others will also leave abnormally (via BrokenBarrierException),
 * until the barrier is <code>restart</code>ed. This is usually the simplest and best strategy for sharing knowledge
 * about failures among cooperating threads in the most common usages contexts of Barriers. This implementation has the
 * property that interruptions among newly arriving threads can cause as-yet-unresumed threads from a previous barrier
 * cycle to return out as broken. This transmits breakage as early as possible, but with the possible byproduct that
 * only some threads returning out of a barrier will realize that it is newly broken. (Others will not realize this
 * until a future cycle.) (The Rendezvous class has a more uniform, but sometimes less desirable policy.)
 * <p>
 * Barriers support an optional Runnable command that is run once per barrier point.
 **/
public class CyclicBarrier implements BuiltinBarrier {

  private final Lock  lock            = new Lock();

  protected final int parties_;
  protected boolean   broken_         = false;
  protected Runnable  barrierCommand_ = null;
  protected int       count_;                      // number of parties still waiting
  protected int       resets_         = 0;         // incremented on each release

  /**
   * Create a CyclicBarrier for the indicated number of parties, and no command to run at each barrier.
   * 
   * @exception IllegalArgumentException if parties less than or equal to zero.
   **/

  public CyclicBarrier(int parties) {
    this(parties, null);
  }

  /**
   * Create a CyclicBarrier for the indicated number of parties. and the given command to run at each barrier point.
   * 
   * @exception IllegalArgumentException if parties less than or equal to zero.
   **/

  public CyclicBarrier(int parties, Runnable command) {
    if (parties <= 0) throw new IllegalArgumentException();
    parties_ = parties;
    count_ = parties;
    barrierCommand_ = command;
  }

  /**
   * Set the command to run at the point at which all threads reach the barrier. This command is run exactly once, by
   * the thread that trips the barrier. The command is not run if the barrier is broken.
   * 
   * @param command the command to run. If null, no command is run.
   * @return the previous command
   **/

  public Runnable setBarrierCommand(Runnable command) {
    lock.writeLock();
    try {
      Runnable old = barrierCommand_;
      barrierCommand_ = command;
      return old;
    } finally {
      lock.writeUnlock();
    }
  }

  public boolean isBroken() {
    lock.writeLock();
    try {
      return broken_;
    } finally {
      lock.writeUnlock();
    }
  }

  public void reset() {
    lock.writeLock();
    try {
      broken_ = false;
      ++resets_;
      count_ = parties_;
      lock.notifyAll();
    } finally {
      lock.writeUnlock();
    }
  }

  public int getParties() {
    return parties_;
  }

  public int await() throws InterruptedException, BrokenBarrierException {
    try {
      return doAwait(false, 0L);
    } catch (TimeoutException toe) {
      throw new Error(toe); // cannot happen;
    }
  }

  public int await(long msecs) throws InterruptedException, TimeoutException, BrokenBarrierException {
    return doAwait(true, msecs);
  }

  protected int doAwait(boolean timed, long msecs) throws InterruptedException, TimeoutException,
      BrokenBarrierException {

    lock.writeLock();
    try {
      int index = --count_;

      if (broken_) {
        throw new BrokenBarrierException("Barrier " + index + " is broken");
      } else if (Thread.interrupted()) {
        broken_ = true;
        lock.notifyAll();
        throw new InterruptedException();
      } else if (index == 0) {
        // tripped
        count_ = parties_;
        ++resets_;
        lock.notifyAll();
        try {
          if (barrierCommand_ != null) barrierCommand_.run();
          return 0;
        } catch (RuntimeException ex) {
          broken_ = true;
          return 0;
        }
      } else if (timed && msecs <= 0) {
        broken_ = true;
        lock.doNotifyAll();
        throw new TimeoutException("Barrier " + index + " timeout out: " + msecs);
      } else { // wait until next reset
        int r = resets_;
        long startTime = (timed) ? System.currentTimeMillis() : 0;
        long waitTime = msecs;
        for (;;) {
          try {
            lock.doWait(waitTime);
          } catch (InterruptedException ex) {
            // Only claim that broken if interrupted before reset
            if (resets_ == r) {
              broken_ = true;
              lock.doNotifyAll();
              throw ex;
            } else {
              Thread.currentThread().interrupt(); // propagate
            }
          }

          if (broken_) throw new BrokenBarrierException("Barrier " + index + " is broken");

          else if (r != resets_) return index;

          else if (timed) {
            waitTime = msecs - (System.currentTimeMillis() - startTime);
            if (waitTime <= 0) {
              broken_ = true;
              lock.notifyAll();
              throw new TimeoutException("Barrier " + index + " timeout out: " + msecs);
            }
          }
        }
      }
    } finally {
      lock.writeUnlock();
    }
  }

  public int getNumberWaiting() {
    lock.writeLock();
    try {
      return parties_ - count_;
    } finally {
      lock.writeUnlock();
    }
  }
}
