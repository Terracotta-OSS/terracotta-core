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

  public void acquireWriteLock(PassthroughEntityTuple entityTuple, PassthroughConnection sender, Runnable onAcquire) {
    LockState state = getStateForName(entityTuple);
    // Write-locks have no clientInstanceID.
    long clientInstanceID = 0;
    Target target = new Target(sender, clientInstanceID, onAcquire);
    state.writeWaits.add(target);
    findAndAwardNewOwners(state);
  }

  public void releaseWriteLock(PassthroughEntityTuple entityTuple, PassthroughConnection sender) {
    LockState state = getStateForName(entityTuple);
    // Write-locks have no clientInstanceID.
    long clientInstanceID = 0;
    Target target = new Target(sender, clientInstanceID, null);
    Assert.assertTrue(state.writeOwner.equals(target));
    state.writeOwner = null;
    findAndAwardNewOwners(state);
  }

  public void acquireReadLock(PassthroughEntityTuple entityTuple, PassthroughConnection sender, long clientInstanceID, Runnable onAcquire) {
    LockState state = getStateForName(entityTuple);
    Target target = new Target(sender, clientInstanceID, onAcquire);
    state.readWaits.add(target);
    findAndAwardNewOwners(state);
  }

  public void releaseReadLock(PassthroughEntityTuple entityTuple, PassthroughConnection sender, long clientInstanceID) {
    LockState state = getStateForName(entityTuple);
    Target target = new Target(sender, clientInstanceID, null);
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
   */
  private void findAndAwardNewOwners(LockState state) {
    // If the write lock is owned, we can't change anything.
    boolean isWriteOwned = (null != state.writeOwner);
    if (!isWriteOwned) {
      // If nobody has the lock we can attempt write owners (which we will prefer).
      boolean isReadOwned = !state.readOwners.isEmpty();
      if (!isReadOwned && !state.writeWaits.isEmpty()) {
        Target newTarget = state.writeWaits.remove(0);
        state.writeOwner = newTarget;
        newTarget.onAcquire.run();
      } else if (!state.readWaits.isEmpty()) {
        while (!state.readWaits.isEmpty()) {
          Target newTarget = state.readWaits.remove(0);
          state.readOwners.add(newTarget);
          newTarget.onAcquire.run();
        }
      }
    }
  }


  private static class Target {
    public final PassthroughConnection sender;
    public final long clientInstanceID;
    public final Runnable onAcquire;
    public Target(PassthroughConnection sender, long clientInstanceID, Runnable onAcquire) {
      this.sender = sender;
      this.clientInstanceID = clientInstanceID;
      this.onAcquire = onAcquire;
    }
    @Override
    public int hashCode() {
      return this.sender.hashCode() ^ (int)this.clientInstanceID;
    }
    @Override
    public boolean equals(Object obj) {
      Target other = (Target)obj;
      // We can instance-compare connections.
      return (this.sender == other.sender) && (this.clientInstanceID == other.clientInstanceID);
    }
  }

  private static class LockState {
    public Target writeOwner;
    public Set<Target> readOwners = new HashSet<Target>();
    public List<Target> writeWaits = new Vector<Target>();
    public List<Target> readWaits = new Vector<Target>();
  }
}
