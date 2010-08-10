/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCSerializable;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.RecallBatchContext;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.object.locks.ThreadIDFactory;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Message for obtaining/releasing locks, and for modifying them (ie. wait/notify)
 * 
 * @author steve
 */
public class LockRequestMessage extends DSOMessageBase implements MultiThreadedEventContext {

  private final static byte LOCK_ID                = 1;
  private final static byte LOCK_LEVEL             = 2;
  private final static byte THREAD_ID              = 3;
  private final static byte REQUEST_TYPE           = 4;
  private final static byte WAIT_MILLIS            = 5;
  private final static byte CONTEXT                = 6;
  private final static byte BATCHED_RECALL_CONTEXT = 7;

  // request types
  public static enum RequestType {
    LOCK, UNLOCK, WAIT, RECALL_COMMIT, QUERY, TRY_LOCK, INTERRUPT_WAIT, BATCHED_RECALL_COMMIT;
  }

  private static final ThreadIDFactory               threadIDFactory = new ThreadIDFactory();

  private final Set<ClientServerExchangeLockContext> contexts        = new LinkedHashSet<ClientServerExchangeLockContext>();
  private final LinkedList<RecallBatchContext>       recallContexts  = new LinkedList<RecallBatchContext>();

  private LockID                                     lockID          = null;
  private ServerLockLevel                            lockLevel       = null;
  private ThreadID                                   threadID        = null;
  private RequestType                                requestType     = null;
  private long                                       waitMillis      = -1;

  public LockRequestMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                            MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public LockRequestMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                            TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(REQUEST_TYPE, (byte) requestType.ordinal());
    switch (requestType) {
      case LOCK:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        break;
      case UNLOCK:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        if (lockLevel != null) {
          putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        }
        break;
      case TRY_LOCK:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        putNVPair(WAIT_MILLIS, waitMillis);
        break;
      case WAIT:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        // putNVPair(LOCK_LEVEL, (byte) lockLevel.ordinal());
        putNVPair(WAIT_MILLIS, waitMillis);
        break;
      case INTERRUPT_WAIT:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        break;
      case QUERY:
        putNVPair(LOCK_ID, lockID);
        putNVPair(THREAD_ID, threadID.toLong());
        break;
      case RECALL_COMMIT:
        putNVPair(LOCK_ID, lockID);
        for (Iterator i = contexts.iterator(); i.hasNext();) {
          putNVPair(CONTEXT, (TCSerializable) i.next());
        }
        break;
      case BATCHED_RECALL_COMMIT:
        for (RecallBatchContext batchContext : recallContexts) {
          putNVPair(BATCHED_RECALL_CONTEXT, batchContext);
        }
        break;
    }
  }

  protected String describePayload() {
    StringBuffer rv = new StringBuffer();

    rv.append("Request Type: ").append(requestType).append('\n');
    rv.append(lockID).append(' ').append(threadID).append(' ').append("Lock Type: ").append(lockLevel).append('\n');

    if (waitMillis >= 0) {
      rv.append("Timeout : ").append(waitMillis).append("ms\n");
    }
    if (contexts.size() > 0) {
      rv.append("Holder/Waiters/Pending contexts size = ").append(contexts.size()).append('\n');
    }
    if (recallContexts.size() > 0) {
      rv.append("RecallCommits contexts size = ").append(recallContexts.size()).append('\n');
    }

    return rv.toString();
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
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
      case THREAD_ID:
        threadID = threadIDFactory.getOrCreate(getLongValue());
        return true;
      case REQUEST_TYPE:
        try {
          requestType = RequestType.values()[getByteValue()];
        } catch (ArrayIndexOutOfBoundsException e) {
          return false;
        }
        return true;
      case WAIT_MILLIS:
        waitMillis = getLongValue();
        return true;
      case CONTEXT:
        contexts.add((ClientServerExchangeLockContext) getObject(new ClientServerExchangeLockContext()));
        return true;
      case BATCHED_RECALL_CONTEXT:
        recallContexts.add((RecallBatchContext) getObject(new RecallBatchContext()));
        return true;
      default:
        return false;
    }
  }

  public RequestType getRequestType() {
    return requestType;
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
    Assert.assertTrue(contexts.add(ctxt));
  }

  public void addRecallBatchContext(RecallBatchContext recallBatchContext) {
    this.recallContexts.add(recallBatchContext);
  }

  public Collection<ClientServerExchangeLockContext> getContexts() {
    return contexts;
  }

  public LinkedList<RecallBatchContext> getRecallBatchedContexts() {
    return recallContexts;
  }

  public long getTimeout() {
    return waitMillis;
  }

  public void initializeInterruptWait(LockID lid, ThreadID id) {
    initialize(lid, id, null, RequestType.INTERRUPT_WAIT, -1);
  }

  public void initializeQuery(LockID lid, ThreadID id) {
    initialize(lid, id, null, RequestType.QUERY, -1);
  }

  public void initializeLock(LockID lid, ThreadID id, ServerLockLevel level) {
    initialize(lid, id, level, RequestType.LOCK, -1);
  }

  public void initializeTryLock(LockID lid, ThreadID id, long timeout, ServerLockLevel level) {
    initialize(lid, id, level, RequestType.TRY_LOCK, timeout);
  }

  public void initializeUnlock(LockID lid, ThreadID id, ServerLockLevel level) {
    initialize(lid, id, level, RequestType.UNLOCK, -1);
  }

  public void initializeWait(LockID lid, ThreadID id, long timeout) {
    initialize(lid, id, null, RequestType.WAIT, timeout);
  }

  public void initializeRecallCommit(LockID lid) {
    initialize(lid, ThreadID.VM_ID, null, RequestType.RECALL_COMMIT, -1);
  }

  public void initializeBatchedRecallCommit() {
    initialize(null, ThreadID.VM_ID, null, RequestType.BATCHED_RECALL_COMMIT, -1);
  }

  private void initialize(LockID lid, ThreadID id, ServerLockLevel level, RequestType reqType, long millis) {
    this.lockID = lid;
    this.lockLevel = level;
    this.threadID = id;
    this.requestType = reqType;
    this.waitMillis = millis;
  }

  public Object getKey() {
    return this.getSourceNodeID();
  }
}
