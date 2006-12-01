/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.impl.GlobalLockInfo;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class LockResponseMessage extends DSOMessageBase {
  private static final byte TYPE              = 1;
  private static final byte THREAD_ID         = 2;
  private static final byte LOCK_ID           = 3;
  private static final byte LOCK_LEVEL        = 7;
  private static final byte GLOBAL_LOCK_INFO  = 8;

  public static final int   LOCK_AWARD        = 1;
  public static final int   LOCK_RECALL       = 2;
  public static final int   LOCK_WAIT_TIMEOUT = 3;
  public static final int   LOCK_INFO         = 4;
  public static final int   LOCK_NOT_AWARDED  = 5;

  private int               type;
  private ThreadID          threadID;
  private LockID            lockID;
  private int               lockLevel;
  private GlobalLockInfo    globalLockInfo;

  public LockResponseMessage(MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel, TCMessageType type) {
    super(monitor, out, channel, type);
  }

  public LockResponseMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                             TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(TYPE, this.type);
    putNVPair(LOCK_ID, this.lockID.asString());
    putNVPair(THREAD_ID, this.threadID.toLong());
    putNVPair(LOCK_LEVEL, this.lockLevel);
    if (globalLockInfo != null) {
      putNVPair(GLOBAL_LOCK_INFO, globalLockInfo);
    }
  }

  protected String describePayload() {
    StringBuffer rv = new StringBuffer();
    rv.append("Type : ");

    if (isLockAward()) {
      rv.append("LOCK AWARD \n");
    } else if (isLockRecall()) {
      rv.append("LOCK RECALL \n");
    } else if (isLockWaitTimeout()) {
      rv.append("LOCK WAIT TIMEOUT \n");
    } else if (isLockInfo()) {
      rv.append("LOCK INFO");
    } else if (isLockNotAwarded()) {
      rv.append("LOCK NOT AWARDED");
    } else {
      rv.append("UNKNOWN \n");
    }

    rv.append(lockID).append(' ').append(threadID).append(' ').append("Lock Type: ").append(
                                                                                            LockLevel
                                                                                                .toString(lockLevel))
        .append('\n');

    return rv.toString();
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case TYPE:
        this.type = getIntValue();
        return true;
      case THREAD_ID:
        // TODO: Make this use a transactionID factory so that we can avoid dups
        this.threadID = new ThreadID(getLongValue());
        return true;
      case LOCK_ID:
        // TODO: Make this use a lockID factory so that we can avoid dups
        lockID = new LockID(getStringValue());
        return true;
      case LOCK_LEVEL:
        this.lockLevel = getIntValue();
        return true;
      case GLOBAL_LOCK_INFO:
        globalLockInfo = new GlobalLockInfo();
        getObject(globalLockInfo);
        return true;
      default:
        return false;
    }
  }

  public boolean isLockAward() {
    return (this.type == LOCK_AWARD);
  }

  public boolean isLockRecall() {
    return (this.type == LOCK_RECALL);
  }

  public boolean isLockWaitTimeout() {
    return (this.type == LOCK_WAIT_TIMEOUT);
  }

  public boolean isLockInfo() {
    return (this.type == LOCK_INFO);
  }

  public boolean isLockNotAwarded() {
    return (this.type == LOCK_NOT_AWARDED);
  }

  public LockID getLockID() {
    return this.lockID;
  }

  public ThreadID getThreadID() {
    return this.threadID;
  }

  public int getLockLevel() {
    return this.lockLevel;
  }

  public GlobalLockInfo getGlobalLockInfo() {
    return globalLockInfo;
  }

  public void initializeLockAward(LockID lid, ThreadID sid, int level) {
    this.type = LOCK_AWARD;
    initialize(lid, sid, level);
  }

  public void initializeLockNotAwarded(LockID lid, ThreadID sid, int level) {
    this.type = LOCK_NOT_AWARDED;
    initialize(lid, sid, level);
  }

  public void initializeLockRecall(LockID lid, ThreadID sid, int level) {
    this.type = LOCK_RECALL;
    initialize(lid, sid, level);
  }

  public void initializeLockWaitTimeout(LockID lid, ThreadID sid, int level) {
    this.type = LOCK_WAIT_TIMEOUT;
    initialize(lid, sid, level);
  }

  public void initializeLockInfo(LockID lid, ThreadID sid, int level, GlobalLockInfo info) {
    this.type = LOCK_INFO;
    initialize(lid, sid, level, info);
  }

  private void initialize(LockID lid, ThreadID sid, int level) {
    initialize(lid, sid, level, null);
  }

  private void initialize(LockID lid, ThreadID sid, int level, GlobalLockInfo info) {
    this.threadID = sid;
    this.lockID = lid;
    this.lockLevel = level;
    this.globalLockInfo = info;
  }

}