/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


/**
 * Entities are accessed view a read-write lock.  Acquiring a maintenance ref acquires the write lock, while closing the ref
 * releases this lock.
 * Fetching/releasing an entity acquires/releases the read-lock.
 * Locks are awarded by calling their given Runnable so all calls into this interface are without return value and merely
 * advance the internal state machine..
 * Note that there are no callbacks for the releases so they release, immediately and never fail.
 * NOTE:  That means the delegate methods are called BEFORE the called function returns.
 * Note that we currently identify the entity locks by the entity name, although we may need to take the class into account,
 * in the future.
 * NOTE:  This class currently assumes that it is being called by the single-threaded connection processor so it may need
 * changes, in the future, if that becomes multi-threaded.
 */
public class PassthroughLockManager {
  private final Map<PassthroughEntityTuple, LockState> locks;

  public PassthroughLockManager() {
    this.locks = new HashMap<PassthroughEntityTuple, LockState>();
  }

  public void acquireWriteLock(PassthroughEntityTuple entityTuple, long clientOriginID, Runnable onAcquire) {
    LockState state = getStateForName(entityTuple);
    // Write-locks have no clientInstanceID.
    long clientInstanceID = 0;
    Target target = new Target(clientOriginID, clientInstanceID, onAcquire);
    state.writeWaits.add(target);
    findAndAwardNewOwners(state);
  }

  public boolean tryAcquireWriteLock(PassthroughEntityTuple entityTuple, long clientOriginID) {
    LockState state = getStateForName(entityTuple);
    // Before we do anything, determine if we get the lock, should we add a write wait.
    boolean canLock = (null == state.writeOwner)
        && (state.readOwners.isEmpty())
        && (state.writeWaits.isEmpty());
    if (canLock) {
      // Write-locks have no clientInstanceID.
      long clientInstanceID = 0;
      CheckLock onAcquire = new CheckLock();
      Target target = new Target(clientOriginID, clientInstanceID, onAcquire);
      state.writeWaits.add(target);
      findAndAwardNewOwners(state);
      onAcquire.check();
    }
    return canLock;
  }

  public void releaseWriteLock(PassthroughEntityTuple entityTuple, long clientOriginID) {
    LockState state = getStateForName(entityTuple);
    // Write-locks have no clientInstanceID.
    long clientInstanceID = 0;
    Target target = new Target(clientOriginID, clientInstanceID, null);
    Assert.assertTrue(state.writeOwner.equals(target));
    state.writeOwner = null;
    findAndAwardNewOwners(state);
  }

  public void restoreWriteLock(PassthroughEntityTuple entityTuple, long clientOriginID, Runnable onAcquire) {
    LockState state = getStateForName(entityTuple);
    // Write-locks have no clientInstanceID.
    long clientInstanceID = 0;
    final Target target = new Target(clientOriginID, clientInstanceID, onAcquire);
    state.writeWaits.add(target);
    boolean wasAwarded = findAndAwardNewOwners(state);
    // We expect that the lock was awarded.
    Assert.assertTrue(wasAwarded);
    // We expect that it was awarded to us.
    Assert.assertTrue(target == state.writeOwner);
  }

  public void acquireReadLock(PassthroughEntityTuple entityTuple, long clientOriginID, long clientInstanceID, Runnable onAcquire) {
    LockState state = getStateForName(entityTuple);
    Target target = new Target(clientOriginID, clientInstanceID, onAcquire);
    state.readWaits.add(target);
    findAndAwardNewOwners(state);
  }

  public void releaseReadLock(PassthroughEntityTuple entityTuple, long clientOriginID, long clientInstanceID) {
    LockState state = getStateForName(entityTuple);
    Target target = new Target(clientOriginID, clientInstanceID, null);
    boolean didRemove = state.readOwners.remove(target);
    Assert.assertTrue(didRemove);
    findAndAwardNewOwners(state);
  }

  private LockState getStateForName(PassthroughEntityTuple entityTuple) {
    LockState state = this.locks.get(entityTuple);
    if (null == state) {
      state = new LockState();
      this.locks.put(entityTuple, state);
    }
    return state;
  }

  /**
   * Called whenever a lock state changes (either someone released or tried to acquire) to determine any ownership change.
   * 
   * @param state
   * @return True if the lock (either write or read) was awarded to anyone (potentially multiple, if this was a read lock)
   * within this call.
   */
  private boolean findAndAwardNewOwners(LockState state) {
    boolean wasAwarded = false;
    // If the write lock is owned, we can't change anything.
    boolean isWriteOwned = (null != state.writeOwner);
    if (!isWriteOwned) {
      // If nobody has the lock we can attempt write owners (which we will prefer).
      boolean isReadOwned = !state.readOwners.isEmpty();
      if (!isReadOwned && !state.writeWaits.isEmpty()) {
        Target newTarget = state.writeWaits.remove(0);
        state.writeOwner = newTarget;
        newTarget.onAcquire.run();
        wasAwarded = true;
      } else if (!state.readWaits.isEmpty()) {
        while (!state.readWaits.isEmpty()) {
          Target newTarget = state.readWaits.remove(0);
          state.readOwners.add(newTarget);
          newTarget.onAcquire.run();
        }
        wasAwarded = true;
      }
    }
    return wasAwarded;
  }


  private static class Target {
    public final long clientOriginID;
    public final long clientInstanceID;
    public final Runnable onAcquire;
    
    public Target(long clientOriginID, long clientInstanceID, Runnable onAcquire) {
      this.clientOriginID = clientOriginID;
      this.clientInstanceID = clientInstanceID;
      this.onAcquire = onAcquire;
    }
    @Override
    public int hashCode() {
      return (int)this.clientOriginID ^ (int)this.clientInstanceID;
    }
    @Override
    public boolean equals(Object obj) {
      Target other = (Target)obj;
      return (this.clientOriginID == other.clientOriginID) && (this.clientInstanceID == other.clientInstanceID);
    }
  }

  private static class LockState {
    public Target writeOwner;
    public Set<Target> readOwners = new HashSet<Target>();
    public List<Target> writeWaits = new Vector<Target>();
    public List<Target> readWaits = new Vector<Target>();
  }


  /**
   * This runnable exists to give a mechanism to ensure that the lock was successful.
   */
  private static class CheckLock implements Runnable {
    private boolean didLock = false;
    public void check() {
      Assert.assertTrue(this.didLock);
    }
    @Override
    public void run() {
      this.didLock = true;
    }
  }
}
