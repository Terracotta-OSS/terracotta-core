/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.state;

public class StateSyncManagerImpl implements StateSyncManager {

  private volatile StateManager stateManager;

  private boolean               objectSyncComplete = false;

  public StateSyncManagerImpl() {
    //
  }

  public void setStateManager(StateManager stateManager) {
    this.stateManager = stateManager;
  }

  public void objectSyncComplete() {
    final boolean move;

    synchronized (this) {
      objectSyncComplete = true;
      move = canMove();
    }

    if (move) {
      moveState();
    }
  }

  public void indexSyncComplete() {
    // overridden in EE
  }

  protected boolean canMove() {
    return isIndexSyncComplete() && isObjectSyncComplete();
  }

  protected boolean isObjectSyncComplete() {
    return objectSyncComplete;
  }

  protected boolean isIndexSyncComplete() {
    // overridden in EE
    return true;
  }

  protected void moveState() {
    this.stateManager.moveToPassiveStandbyState();
  }

}
