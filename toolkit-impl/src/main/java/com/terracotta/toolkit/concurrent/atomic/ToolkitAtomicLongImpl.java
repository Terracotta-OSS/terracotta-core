/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.concurrent.atomic;

import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.rejoin.RejoinCallback;
import com.terracotta.toolkit.util.ToolkitIDGenerator;
import com.terracotta.toolkit.util.ToolkitObjectStatusImpl;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class ToolkitAtomicLongImpl implements ToolkitAtomicLong, RejoinCallback {
  private final ToolkitStore<String, ToolkitAtomicLongState> atomicLongs;
  private final String                                       name;
  private final ToolkitLock                                  lock;
  private final long                                         uid;
  private final ToolkitIDGenerator                           longIdGenerator;
  private final ToolkitObjectStatusImpl                      status;
  private final AtomicInteger                                currentRejoinCount = new AtomicInteger();

  public ToolkitAtomicLongImpl(String name, ToolkitStore<String, ToolkitAtomicLongState> clusteredMap,
                               ToolkitIDGenerator longIdGenerator, PlatformService platformService) {
    this.atomicLongs = clusteredMap;
    this.name = name;
    this.lock = atomicLongs.createLockForKey(name).writeLock();
    ToolkitAtomicLongState state = atomicLongs.get(name);
    if (state == null) {
      long tmpUid = longIdGenerator.getId();
      ToolkitAtomicLongState tmpState = new ToolkitAtomicLongState(tmpUid, new Long(0));
      state = atomicLongs.putIfAbsent(name, tmpState);
      if (state == null) {
        state = tmpState;
      }
    }
    this.longIdGenerator = longIdGenerator;
    this.uid = state.getUid();
    this.status = new ToolkitObjectStatusImpl(platformService);
    this.currentRejoinCount.set(status.getCurrentRejoinCount());
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
  public boolean isDestroyed() {
    return getInternalStateOrNullIfDestroyed() == null;
  }

  private ToolkitAtomicLongState getInternalStateOrNullIfDestroyed() {
    ToolkitAtomicLongState state = atomicLongs.get(name);
    if (state != null && state.getUid() != uid) {
      // state found, but created with different uid -> destroyed
      return null;
    } else {
      return state;
    }
  }

  private ToolkitAtomicLongState getInternalState() {
    ToolkitAtomicLongState state = getInternalStateOrNullIfDestroyed();
    if (state == null) throw new IllegalStateException("ToolkitAtomicLong with name '" + name
                                                       + "' is already destroyed and no longer exists!");
    return state;
  }

  @Override
  public long addAndGet(long delta) {
    lock.lock();
    try {
      ToolkitAtomicLongState state = getInternalState();
      long val = state.getLongValue();
      Long result = val + delta;
      atomicLongs.putNoReturn(name, state.setLongValue(result));
      return result;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean compareAndSet(long expect, long update) {
    lock.lock();
    try {
      ToolkitAtomicLongState state = getInternalState();
      long val = state.getLongValue();
      if (val == expect) {
        atomicLongs.putNoReturn(name, state.setLongValue(update));
        return true;
      } else {
        return false;
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long decrementAndGet() {
    lock.lock();
    try {
      return addAndGet(-1);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long get() {
    return getInternalState().getLongValue();
  }

  @Override
  public String toString() {
    return Long.toString(get());
  }

  @Override
  public long getAndAdd(long delta) {
    lock.lock();
    try {
      ToolkitAtomicLongState state = getInternalState();
      long val = state.getLongValue();
      Long result = val + delta;
      atomicLongs.putNoReturn(name, state.setLongValue(result));
      return val;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long getAndDecrement() {
    lock.lock();
    try {
      ToolkitAtomicLongState state = getInternalState();
      long val = state.getLongValue();
      Long result = val - 1;
      atomicLongs.putNoReturn(name, state.setLongValue(result));
      return val;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long getAndIncrement() {
    lock.lock();
    try {
      ToolkitAtomicLongState state = getInternalState();
      long val = state.getLongValue();
      Long result = val + 1;
      atomicLongs.putNoReturn(name, state.setLongValue(result));
      return val;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long getAndSet(long newValue) {
    lock.lock();
    try {
      ToolkitAtomicLongState state = getInternalState();
      long val = state.getLongValue();
      atomicLongs.putNoReturn(name, state.setLongValue(newValue));
      return val;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long incrementAndGet() {
    lock.lock();
    try {
      return addAndGet(1);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void set(long newValue) {
    lock.lock();
    try {
      ToolkitAtomicLongState state = getInternalState();
      atomicLongs.putNoReturn(name, state.setLongValue(newValue));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public byte byteValue() {
    return (byte) get();
  }

  @Override
  public short shortValue() {
    return (short) get();
  }

  @Override
  public int intValue() {
    return (int) get();
  }

  @Override
  public long longValue() {
    return get();
  }

  @Override
  public float floatValue() {
    return get();
  }

  @Override
  public double doubleValue() {
    return get();
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void destroy() {
    if (isDestroyed()) { return; }
    longIdGenerator.incrementId();
    lock.lock();
    try {
      if (isDestroyed()) { return; }
      atomicLongs.remove(name);
    } finally {
      lock.unlock();
    }
  }

  public static class ToolkitAtomicLongState implements Serializable {

    private volatile Long longValue;
    private final long    uid;

    public ToolkitAtomicLongState(long uid, Long longValue) {
      this.longValue = longValue;
      this.uid = uid;
    }

    public long getUid() {
      return uid;
    }

    public long getLongValue() {
      return longValue.longValue();
    }

    public ToolkitAtomicLongState setLongValue(Long longValue) {
      this.longValue = longValue;
      return this;
    }

  }
}
