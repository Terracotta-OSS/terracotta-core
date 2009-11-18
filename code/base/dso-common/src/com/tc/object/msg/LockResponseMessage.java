/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class LockResponseMessage extends DSOMessageBase implements MultiThreadedEventContext {

  private static final byte TYPE              = 1;
  private static final byte THREAD_ID         = 2;
  private static final byte LOCK_ID           = 3;
  private static final byte LOCK_LEVEL        = 4;
  private static final byte CONTEXT           = 5;
  private static final byte LOCK_LEASE_MILLIS = 6;

  public static enum ResponseType {
    AWARD, RECALL, RECALL_WITH_TIMEOUT, WAIT_TIMEOUT, INFO, REFUSE;
  }

  private final Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();

  private ResponseType                                      responseType;
  private ThreadID                                          threadID;
  private LockID                                            lockID;
  private ServerLockLevel                                   lockLevel;
  private int                                               leaseTimeInMs;

  public LockResponseMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                             MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public LockResponseMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                             TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(TYPE, (byte) responseType.ordinal());
    switch (responseType) {
      case AWARD:
      case REFUSE:
      case RECALL:
      case WAIT_TIMEOUT:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        break;
      case RECALL_WITH_TIMEOUT:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        putNVPair(LOCK_LEASE_MILLIS, leaseTimeInMs);
        break;
      case INFO:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        if (lockLevel != null) {
          putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        }
        for (ClientServerExchangeLockContext cselc : contexts) {
          putNVPair(CONTEXT, cselc);
        }
        break;
    }
  }

  protected String describePayload() {
    StringBuffer rv = new StringBuffer();
    rv.append("Type : ").append(responseType).append('\n');
    rv.append(lockID).append(' ').append(threadID).append(' ').append("Lock Type: ").append(lockLevel).append('\n');
    return rv.toString();
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case TYPE:
        try {
          responseType = ResponseType.values()[getByteValue()];
        } catch (ArrayIndexOutOfBoundsException e) {
          return false;
        }
        return true;
      case THREAD_ID:
        // TODO: Make this use a transactionID factory so that we can avoid dups
        threadID = new ThreadID(getLongValue());
        return true;
      case LOCK_ID:
        // TODO: Make this use a lockID factory so that we can avoid dups
        lockID = getLockIDValue();
        return true;
      case LOCK_LEVEL:
        try {
          lockLevel = ServerLockLevel.values()[getByteValue()];
        } catch (ArrayIndexOutOfBoundsException e) {
          return false;
        }
        return true;
      case CONTEXT:
        contexts.add((ClientServerExchangeLockContext) getObject(new ClientServerExchangeLockContext()));
        return true;
      case LOCK_LEASE_MILLIS:
        leaseTimeInMs = getIntValue();
        return true;
      default:
        return false;
    }
  }

  public ResponseType getResponseType() {
    return responseType;
  }

  public int getAwardLeaseTime() {
    return leaseTimeInMs;
  }

  public LockID getLockID() {
    return lockID;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public ServerLockLevel getLockLevel() {
    return lockLevel;
  }

  public void addContext(ClientServerExchangeLockContext ctxt) {
    contexts.add(ctxt);
  }

  public Collection<ClientServerExchangeLockContext> getContexts() {
    return contexts;
  }

  public void initializeAward(LockID lid, ThreadID sid, ServerLockLevel level) {
    initialize(ResponseType.AWARD, lid, sid, level, -1);
  }

  public void initializeRefuse(LockID lid, ThreadID sid, ServerLockLevel level) {
    initialize(ResponseType.REFUSE, lid, sid, level, -1);
  }

  public void initializeRecallWithTimeout(LockID lid, ThreadID sid, ServerLockLevel level, int leaseTimeInMillis) {
    initialize(ResponseType.RECALL_WITH_TIMEOUT, lid, sid, level, leaseTimeInMillis);
  }

  public void initializeRecall(LockID lid, ThreadID sid, ServerLockLevel level) {
    initialize(ResponseType.RECALL, lid, sid, level, -1);
  }

  public void initializeWaitTimeout(LockID lid, ThreadID sid, ServerLockLevel level) {
    initialize(ResponseType.WAIT_TIMEOUT, lid, sid, level, -1);
  }

  public void initializeLockInfo(LockID lid, ThreadID sid, ServerLockLevel level) {
    initialize(ResponseType.INFO, lid, sid, level, -1);
  }

  private void initialize(ResponseType requestType, LockID lid, ThreadID sid, ServerLockLevel level,
                          int leaseTimeInMills) {
    this.responseType = requestType;
    this.threadID = sid;
    this.lockID = lid;
    this.lockLevel = level;
    this.leaseTimeInMs = leaseTimeInMills;
  }

  public Object getKey() {
    return this.getSourceNodeID();
  }

}
