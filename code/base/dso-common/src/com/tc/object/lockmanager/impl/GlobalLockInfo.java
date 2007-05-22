/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.lockmanager.api.LockID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class is used to hold the gloabl information of an lock object and passed back to the client when a client
 * queries about the information of a lock.
 */
public class GlobalLockInfo implements TCSerializable {
  private LockID     lockID;
  private int        level;
  private int        lockRequestQueueLength;
  private int        lockUpgradeQueueLength;
  private Collection greedyHoldersInfo;
  private Collection holdersInfo;
  private Collection waitersInfo;

  public GlobalLockInfo() {
    super();
  }

  public GlobalLockInfo(LockID lockID, int level, int lockRequestQueueLength, int lockUpgradeQueueLength,
                        Collection greedyHolders, Collection holders, Collection waiters) {
    this.lockID = lockID;
    this.level = level;
    this.lockRequestQueueLength = lockRequestQueueLength;
    this.lockUpgradeQueueLength = lockUpgradeQueueLength;
    this.greedyHoldersInfo = greedyHolders;
    this.holdersInfo = holders;
    this.waitersInfo = waiters;
  }

  public int getLockRequestQueueLength() {
    return lockRequestQueueLength;
  }

  public int getLockUpgradeQueueLength() {
    return lockUpgradeQueueLength;
  }

  public boolean isLocked(int level) {
    return (this.level == level)
           && ((holdersInfo != null && holdersInfo.size() > 0) || (greedyHoldersInfo != null && greedyHoldersInfo
               .size() > 0));
  }

  public Collection getGreedyHoldersInfo() {
    return greedyHoldersInfo;
  }

  public Collection getHoldersInfo() {
    return holdersInfo;
  }

  public Collection getWaitersInfo() {
    return waitersInfo;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeString(lockID.asString());
    serialOutput.writeInt(level);
    serialOutput.writeInt(lockRequestQueueLength);
    serialOutput.writeInt(lockUpgradeQueueLength);
    serialOutput.writeInt(holdersInfo.size());
    for (Iterator i = holdersInfo.iterator(); i.hasNext();) {
      GlobalLockStateInfo holderInfo = (GlobalLockStateInfo) i.next();
      holderInfo.serializeTo(serialOutput);
    }
    serialOutput.writeInt(greedyHoldersInfo.size());
    for (Iterator i = greedyHoldersInfo.iterator(); i.hasNext();) {
      GlobalLockStateInfo holderInfo = (GlobalLockStateInfo) i.next();
      holderInfo.serializeTo(serialOutput);
    }
    serialOutput.writeInt(waitersInfo.size());
    for (Iterator i = waitersInfo.iterator(); i.hasNext();) {
      GlobalLockStateInfo holderInfo = (GlobalLockStateInfo) i.next();
      holderInfo.serializeTo(serialOutput);
    }
  }

  public Object deserializeFrom(TCByteBufferInputStream serialInput) throws IOException {
    this.lockID = new LockID(serialInput.readString());
    this.level = serialInput.readInt();
    this.lockRequestQueueLength = serialInput.readInt();
    this.lockUpgradeQueueLength = serialInput.readInt();
    int size = serialInput.readInt();
    holdersInfo = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      GlobalLockStateInfo holderInfo = new GlobalLockStateInfo();
      holderInfo.deserializeFrom(serialInput);
      holdersInfo.add(holderInfo);
    }
    size = serialInput.readInt();
    greedyHoldersInfo = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      GlobalLockStateInfo holderInfo = new GlobalLockStateInfo();
      holderInfo.deserializeFrom(serialInput);
      greedyHoldersInfo.add(holderInfo);
    }
    size = serialInput.readInt();
    waitersInfo = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      GlobalLockStateInfo holderInfo = new GlobalLockStateInfo();
      holderInfo.deserializeFrom(serialInput);
      waitersInfo.add(holderInfo);
    }
    return this;
  }

}
