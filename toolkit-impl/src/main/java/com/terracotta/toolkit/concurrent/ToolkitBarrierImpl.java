/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent;

import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.rejoin.RejoinCallback;
import com.terracotta.toolkit.util.ToolkitIDGenerator;
import com.terracotta.toolkit.util.ToolkitObjectStatusImpl;

import java.io.Serializable;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;

public class ToolkitBarrierImpl implements ToolkitBarrier, RejoinCallback {
  private final String                                    name;
  private final ToolkitStore<String, ToolkitBarrierState> barriers;
  private final int                                       parties;
  private final ToolkitLock                               lock;
  private final long                                      uid;
  private final ToolkitIDGenerator                        longIdGenerator;
  private final ToolkitObjectStatusImpl                   status;
  private final AtomicInteger                             currentRejoinCount = new AtomicInteger();
  private final static TCLogger                           LOGGER             = TCLogging
                                                                                 .getLogger(ToolkitBarrierImpl.class);

  public ToolkitBarrierImpl(String name, int parties, ToolkitStore<String, ToolkitBarrierState> clusteredMap,
                            ToolkitIDGenerator barrierIdGenerator, PlatformService platformService) {
    this.barriers = clusteredMap;
    this.name = name;
    this.parties = parties;
    lock = barriers.createLockForKey(name).writeLock();
    ToolkitBarrierState state = barriers.get(name);
    if (state == null) {
      long tmpUid = barrierIdGenerator.getId();
      ToolkitBarrierState tmpState = new ToolkitBarrierState(name, parties, 0, false, tmpUid);
      state = barriers.putIfAbsent(name, tmpState);
      if (state == null) {
        state = tmpState;
      }
    }
    if (state.getParties() != parties) { throw new IllegalArgumentException(
                                                                            "ClusteredBarrier already exists for name '"
                                                                                + name
                                                                                + "' with different number of parties - "
                                                                                + "requested:" + parties + " existing:"
                                                                                + state.getParties()); }
    this.uid = state.getUid();
    this.longIdGenerator = barrierIdGenerator;
    status = new ToolkitObjectStatusImpl(platformService);
    currentRejoinCount.set(status.getCurrentRejoinCount());
  }

  @Override
  public boolean isDestroyed() {
    return getInternalStateOrNullIfDestroyed() == null;
  }

  private ToolkitBarrierState getInternalStateOrNullIfDestroyed() {
    ToolkitBarrierState state = barriers.get(name);
    if (state != null && state.getUid() != uid) {
      LOGGER.info("barrier found with different UID, expectedUID: " + uid + " foundUID: " + state.getUid());
      return null;
    } else {
      return state;
    }
  }

  private ToolkitBarrierState getInternalState() {
    ToolkitBarrierState rv = getInternalStateOrNullIfDestroyed();
    if (rv == null) { throw new IllegalStateException("ToolkitBarrier with name '" + name
                                                      + "' is already destroyed and no longer exists!"); }
    return rv;
  }

  @Override
  public void rejoinStarted() {
    //
  }

  @Override
  public void rejoinCompleted() {
    //
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getParties() {
    return parties;
  }

  @Override
  public boolean isBroken() {
    return getInternalState().isBroken();
  }

  @Override
  public int await() throws InterruptedException, BrokenBarrierException {
    try {
      return doAwaitInternal(false, 0L);
    } catch (TimeoutException toe) {
      throw new AssertionError(toe); // cannot happen;
    }
  }

  @Override
  public int await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, BrokenBarrierException {
    return doAwaitInternal(true, unit.toMillis(timeout));
  }

  private int doAwaitInternal(boolean timed, long msecs) throws InterruptedException, TimeoutException,
      BrokenBarrierException {
    lock.lock();
    try {
      ToolkitBarrierState state = getInternalState();
      int index = state.decrementCount();
      barriers.putNoReturn(name, state);
      Condition awaitCondition = lock.getCondition();
      if (state.isBroken()) {
        throw new BrokenBarrierException("ClusteredBarrier with name: " + name + "  and index: " + index + " is broken");
      } else if (Thread.interrupted()) {
        state.setBroken(true);
        barriers.putNoReturn(name, state);
        awaitCondition.signalAll();
        throw new InterruptedException();
      } else if (index == 0) { // tripped
        state.setCount(state.getParties());
        state.incrementResetCount();
        barriers.putNoReturn(name, state);
        awaitCondition.signalAll();
        return index;
      } else if (timed && msecs <= 0) {
        state.setBroken(true);
        barriers.putNoReturn(name, state);
        awaitCondition.signalAll();
        throw new TimeoutException("ClusteredBarrier with name: " + name + " and index: " + index + " timed out: "
                                   + msecs);
      } else { // wait until next reset
        int r = state.getResets();
        long startTime = (timed) ? System.currentTimeMillis() : 0;
        long waitTime = msecs;
        while (true) {
          try {
            awaitCondition.await(waitTime, TimeUnit.MILLISECONDS);
          } catch (InterruptedException ex) {
            // Only claim that broken if interrupted before reset
            state = getInternalState();
            if (state.getResets() == r) {
              state.setBroken(true);
              barriers.putNoReturn(name, state);
              awaitCondition.signalAll();
              throw ex;
            } else {
              Thread.currentThread().interrupt(); // propagate
            }
          }
          state = getInternalState();
          if (r != state.getResets()) {
            return index;
          } else if (state.isBroken()) {
            throw new BrokenBarrierException("ClusteredBarrier with name: " + name + " and index: " + index
                                             + " is broken");
          } else if (timed) {
            waitTime = msecs - (System.currentTimeMillis() - startTime);
            if (waitTime <= 0) {
              state.setBroken(true);
              barriers.putNoReturn(name, state);
              awaitCondition.signalAll();
              throw new TimeoutException("ClusteredBarrier with name: " + name + "  and index: " + index
                                         + " timed out: " + msecs);
            }
          }
        }
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void reset() {
    lock.lock();
    try {
      Condition awaitCondition = lock.getCondition();
      ToolkitBarrierState state = getInternalState();
      state.setBroken(false);
      state.incrementResetCount();
      state.setCount(state.getParties());
      barriers.putNoReturn(name, state);
      awaitCondition.signalAll();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void destroy() {
    if (isDestroyed()) { return; }
    longIdGenerator.incrementId();
    lock.lock();
    try {
      if (isDestroyed()) { return; }

      ToolkitBarrierState state = barriers.get(name);
      if (state.getCount() != state.getParties()) { throw new IllegalStateException(
                                                                                    "Not able to destroy ClusteredBarrier "
                                                                                        + name
                                                                                        + " because there are some other clients still waiting for this barrier"); }

      barriers.remove(name);
      Condition awaitCondition = lock.getCondition();
      awaitCondition.signalAll();
    } finally {
      lock.unlock();
    }
  }

  private static class ToolkitBarrierState implements Serializable {
    private final int  totalParties;
    private int        resetCount;
    private boolean    broken;
    private int        count;
    private final long uid;

    public ToolkitBarrierState(String name, int parties, int resetCount, boolean broken, long uid) {
      if (parties <= 0) { throw new IllegalArgumentException("Barrier " + name + " can not have parties " + parties); }
      this.totalParties = parties;
      this.count = parties;
      this.resetCount = resetCount;
      this.broken = broken;
      this.uid = uid;
    }

    public long getUid() {
      return uid;
    }

    public int getResets() {
      return resetCount;
    }

    public void incrementResetCount() {
      resetCount++;
    }

    public void setCount(int c) {
      this.count = c;
    }

    public void setBroken(boolean b) {
      this.broken = b;
    }

    public int getCount() {
      return count;
    }

    public int decrementCount() {
      return --count;
    }

    public boolean isBroken() {
      return broken;
    }

    public int getParties() {
      return totalParties;
    }

    @Override
    public String toString() {
      return "ClusteredBarrierState [totalParties=" + totalParties + ", resetCount=" + resetCount + ", broken="
             + broken + ", count=" + count + ", uid=" + uid + "]";
    }

  }

}
