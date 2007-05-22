/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.session.SessionID;
import com.tc.object.tx.WaitInvocation;
import com.tc.object.tx.WaitInvocationFactory;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Message for obtaining/releasing locks, and for modifying them (ie. wait/notify)
 * 
 * @author steve
 */
public class LockRequestMessage extends DSOMessageBase {

  private static final WaitInvocationFactory waitInvocationFactory           = new WaitInvocationFactory();

  private final static byte                  LOCK_ID                         = 1;
  private final static byte                  LOCK_LEVEL                      = 2;
  private final static byte                  THREAD_ID                       = 3;
  private final static byte                  REQUEST_TYPE                    = 4;
  private final static byte                  WITH_WAIT                       = 5;
  private final static byte                  WAIT_MILLIS                     = 6;
  private final static byte                  WAIT_NANOS                      = 7;
  private final static byte                  NOTIFY_ALL                      = 8;
  private static final byte                  WAIT_ARG_COUNT                  = 9;
  private static final byte                  WAIT_CONTEXT                    = 10;
  private static final byte                  LOCK_CONTEXT                    = 11;
  private static final byte                  PENDING_LOCK_CONTEXT            = 12;
  private static final byte                  PENDING_TRY_LOCK_CONTEXT        = 13;

  // request types
  private final static byte                  UNITIALIZED_REQUEST_TYPE        = -1;
  private final static byte                  OBTAIN_LOCK_REQUEST_TYPE        = 1;
  private final static byte                  RELEASE_LOCK_REQUEST_TYPE       = 2;
  private final static byte                  RECALL_COMMIT_LOCK_REQUEST_TYPE = 3;
  private final static byte                  QUERY_LOCK_REQUEST_TYPE         = 4;
  private final static byte                  TRY_OBTAIN_LOCK_REQUEST_TYPE    = 5;
  private final static byte                  INTERRUPT_WAIT_REQUEST_TYPE     = 6;

  public final static int                    UNITIALIZED_WAIT_TIME           = -1;

  private final Set                          lockContexts                    = new HashSet();
  private final Set                          waitContexts                    = new HashSet();
  private final Set                          pendingLockContexts             = new HashSet();
  private final List                         pendingTryLockContexts          = new ArrayList();

  private LockID                             lockID                          = LockID.NULL_ID;
  private int                                lockLevel                       = LockLevel.NIL_LOCK_LEVEL;
  private ThreadID                           threadID                        = ThreadID.NULL_ID;
  private byte                               requestType                     = UNITIALIZED_REQUEST_TYPE;
  private boolean                            withWait;
  private long                               waitMillis                      = UNITIALIZED_WAIT_TIME;
  private int                                waitNanos                       = UNITIALIZED_WAIT_TIME;
  private boolean                            notifyAll;
  private int                                waitArgCount                    = -1;

  public LockRequestMessage(MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel, TCMessageType type) {
    super(monitor, out, channel, type);
  }

  public LockRequestMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                            TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(LOCK_ID, lockID.asString());
    putNVPair(LOCK_LEVEL, lockLevel);
    putNVPair(THREAD_ID, threadID.toLong());

    putNVPair(REQUEST_TYPE, requestType);

    putNVPair(WITH_WAIT, withWait);

    if (withWait || isTryObtainLockRequest()) {
      putNVPair(WAIT_ARG_COUNT, this.waitArgCount);
      putNVPair(WAIT_MILLIS, waitMillis);
      putNVPair(WAIT_NANOS, waitNanos);
    }

    putNVPair(NOTIFY_ALL, notifyAll);

    for (Iterator i = lockContexts.iterator(); i.hasNext();) {
      putNVPair(LOCK_CONTEXT, (TCSerializable) i.next());
    }

    for (Iterator i = waitContexts.iterator(); i.hasNext();) {
      putNVPair(WAIT_CONTEXT, (TCSerializable) i.next());
    }

    for (Iterator i = pendingLockContexts.iterator(); i.hasNext();) {
      putNVPair(PENDING_LOCK_CONTEXT, (TCSerializable) i.next());
    }

    for (Iterator i = pendingTryLockContexts.iterator(); i.hasNext();) {
      putNVPair(PENDING_TRY_LOCK_CONTEXT, (TCSerializable) i.next());
    }
  }

  private static boolean isValidRequestType(byte type) {
    if ((type == RELEASE_LOCK_REQUEST_TYPE) || (type == OBTAIN_LOCK_REQUEST_TYPE)
        || (type == RECALL_COMMIT_LOCK_REQUEST_TYPE) || (type == QUERY_LOCK_REQUEST_TYPE)
        || (type == TRY_OBTAIN_LOCK_REQUEST_TYPE) || (type == INTERRUPT_WAIT_REQUEST_TYPE)) { return true; }

    return false;
  }

  private static String getRequestTypeDescription(byte type) {
    switch (type) {
      case RELEASE_LOCK_REQUEST_TYPE:
        return "Lock Release";
      case OBTAIN_LOCK_REQUEST_TYPE:
        return "Obtain Lock";
      case RECALL_COMMIT_LOCK_REQUEST_TYPE:
        return "Recall Lock Commit";
      case QUERY_LOCK_REQUEST_TYPE:
        return "Query Lock";
      case TRY_OBTAIN_LOCK_REQUEST_TYPE:
        return "Try Obtain Lock";
      case INTERRUPT_WAIT_REQUEST_TYPE:
        return "Interrupt Wait";
      default:
        return "UNKNOWN (" + type + ")";
    }
  }

  protected String describePayload() {
    StringBuffer rv = new StringBuffer();

    rv.append("Request Type: ").append(getRequestTypeDescription(this.requestType)).append('\n');
    rv.append(lockID).append(' ').append(threadID).append(' ').append("Lock Type: ").append(
                                                                                            LockLevel
                                                                                                .toString(lockLevel))
        .append('\n');

    if (isWaitRelease()) {
      rv.append("Wait millis: ").append(waitMillis).append(", nanos: ").append(waitNanos).append('\n');
    }
    if (waitContexts.size() > 0) {
      rv.append("Wait contexts size = ").append(waitContexts.size()).append('\n');
    }
    if (lockContexts.size() > 0) {
      rv.append("Lock contexts size = ").append(lockContexts.size()).append('\n');
    }
    if (pendingLockContexts.size() > 0) {
      rv.append("Pending Lock contexts size = ").append(pendingLockContexts.size()).append('\n');
    }

    return rv.toString();
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case LOCK_ID:
        // TODO: Make this use a lockID factory so that we can avoid dups
        lockID = new LockID(getStringValue());
        return true;
      case LOCK_LEVEL:
        lockLevel = getIntValue();
        return true;
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      case WITH_WAIT:
        withWait = getBooleanValue();
        return true;
      case REQUEST_TYPE:
        final byte req = getByteValue();
        if (!isValidRequestType(req)) { return false; }
        requestType = req;
        return true;
      case WAIT_MILLIS:
        waitMillis = getLongValue();
        return true;
      case WAIT_NANOS:
        waitNanos = getIntValue();
        return true;
      case NOTIFY_ALL:
        notifyAll = getBooleanValue();
        return true;
      case WAIT_ARG_COUNT:
        waitArgCount = getIntValue();
        return true;
      case LOCK_CONTEXT:
        lockContexts.add(getObject(new LockContext()));
        return true;
      case WAIT_CONTEXT:
        waitContexts.add(getObject(new WaitContext()));
        return true;
      case PENDING_LOCK_CONTEXT:
        pendingLockContexts.add(getObject(new LockContext()));
        return true;
      case PENDING_TRY_LOCK_CONTEXT:
        pendingTryLockContexts.add(getObject(new TryLockContext()));
        return true;
      default:
        return false;
    }
  }

  public LockID getLockID() {
    return lockID;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public int getLockLevel() {
    return lockLevel;
  }

  public boolean isNotifyAll() {
    return notifyAll;
  }

  public void addLockContext(LockContext ctxt) {
    synchronized (lockContexts) {
      lockContexts.add(ctxt);
    }
  }

  public Collection getLockContexts() {
    synchronized (lockContexts) {
      return new HashSet(lockContexts);
    }
  }

  public void addWaitContext(WaitContext ctxt) {
    synchronized (waitContexts) {
      waitContexts.add(ctxt);
    }
  }

  public Collection getWaitContexts() {
    synchronized (waitContexts) {
      return new HashSet(waitContexts);
    }
  }

  public void addPendingLockContext(LockContext ctxt) {
    synchronized (pendingLockContexts) {
      pendingLockContexts.add(ctxt);
    }
  }

  public Collection getPendingLockContexts() {
    synchronized (pendingLockContexts) {
      return new HashSet(pendingLockContexts);
    }
  }

  public void addPendingTryLockContext(LockContext ctxt) {
    Assert.eval(ctxt instanceof TryLockContext);
    synchronized (pendingTryLockContexts) {
      pendingTryLockContexts.add(ctxt);
    }
  }

  public Collection getPendingTryLockContexts() {
    synchronized (pendingTryLockContexts) {
      return new ArrayList(pendingTryLockContexts);
    }
  }

  public boolean isInterruptWaitRequest() {
    if (!isValidRequestType(requestType)) { throw new AssertionError("Invalid request type: " + requestType); }
    return requestType == INTERRUPT_WAIT_REQUEST_TYPE;
  }

  public boolean isQueryLockRequest() {
    if (!isValidRequestType(requestType)) { throw new AssertionError("Invalid request type: " + requestType); }
    return requestType == QUERY_LOCK_REQUEST_TYPE;
  }

  public boolean isTryObtainLockRequest() {
    if (!isValidRequestType(requestType)) { throw new AssertionError("Invalid request type: " + requestType); }
    return requestType == TRY_OBTAIN_LOCK_REQUEST_TYPE;
  }

  public boolean isObtainLockRequest() {
    if (!isValidRequestType(requestType)) { throw new AssertionError("Invalid request type: " + requestType); }
    return requestType == OBTAIN_LOCK_REQUEST_TYPE;
  }

  public boolean isReleaseLockRequest() {
    if (!isValidRequestType(requestType)) { throw new AssertionError("Invalid request type: " + requestType); }
    return requestType == RELEASE_LOCK_REQUEST_TYPE;
  }

  public boolean isRecallCommitLockRequest() {
    if (!isValidRequestType(requestType)) { throw new AssertionError("Invalid request type: " + requestType); }
    return requestType == RECALL_COMMIT_LOCK_REQUEST_TYPE;
  }

  public WaitInvocation getWaitInvocation() {
    if (!this.withWait && !isTryObtainLockRequest()) { throw new IllegalStateException("not a wait request"); }
    return waitInvocationFactory.newWaitInvocation(this.waitArgCount, this.waitMillis, this.waitNanos);
  }

  public boolean isWaitRelease() {
    return this.withWait;
  }

  public void initializeInterruptWait(LockID lid, ThreadID id) {
    initialize(lid, id, LockLevel.NIL_LOCK_LEVEL, INTERRUPT_WAIT_REQUEST_TYPE, false, false, UNITIALIZED_WAIT_TIME,
               UNITIALIZED_WAIT_TIME, -1);
  }

  public void initializeQueryLock(LockID lid, ThreadID id) {
    initialize(lid, id, LockLevel.NIL_LOCK_LEVEL, QUERY_LOCK_REQUEST_TYPE, false, false, UNITIALIZED_WAIT_TIME,
               UNITIALIZED_WAIT_TIME, -1);
  }

  public void initializeObtainLock(LockID lid, ThreadID id, int type) {
    initialize(lid, id, type, OBTAIN_LOCK_REQUEST_TYPE, false, false, UNITIALIZED_WAIT_TIME, UNITIALIZED_WAIT_TIME, -1);
  }

  public void initializeTryObtainLock(LockID lid, ThreadID id, WaitInvocation timeout, int type) {
    initialize(lid, id, type, TRY_OBTAIN_LOCK_REQUEST_TYPE, false, false, timeout.getMillis(), timeout.getNanos(),
               timeout.getSignature().getArgCount());
  }

  public void initializeLockRelease(LockID lid, ThreadID id) {
    initialize(lid, id, LockLevel.NIL_LOCK_LEVEL, RELEASE_LOCK_REQUEST_TYPE, false, false, UNITIALIZED_WAIT_TIME,
               UNITIALIZED_WAIT_TIME, -1);
  }

  public void initializeLockReleaseWait(LockID lid, ThreadID id, WaitInvocation call) {
    initialize(lid, id, LockLevel.NIL_LOCK_LEVEL, RELEASE_LOCK_REQUEST_TYPE, true, false, call.getMillis(), call
        .getNanos(), call.getSignature().getArgCount());
  }

  public void initializeLockRecallCommit(LockID lid) {
    initialize(lid, ThreadID.VM_ID, LockLevel.NIL_LOCK_LEVEL, RECALL_COMMIT_LOCK_REQUEST_TYPE, false, false,
               UNITIALIZED_WAIT_TIME, UNITIALIZED_WAIT_TIME, -1);
  }

  private void initialize(LockID lid, ThreadID id, int level, byte reqType, boolean wait, boolean all, long millis,
                          int nanos, int waitArgs) {
    this.lockID = lid;
    this.lockLevel = level;
    this.threadID = id;

    if (!isValidRequestType(reqType)) { throw new IllegalArgumentException("Invalid request type: " + reqType); }
    this.requestType = reqType;

    if (this.requestType == RELEASE_LOCK_REQUEST_TYPE || this.requestType == RECALL_COMMIT_LOCK_REQUEST_TYPE) {
      if (this.lockLevel != LockLevel.NIL_LOCK_LEVEL) {
        // make the formatter happy
        throw new IllegalArgumentException("Cannot specify a lock level for release or recall commit(yet)");
      }
    }

    this.withWait = wait;
    this.notifyAll = all;

    if (wait || isTryObtainLockRequest()) {
      if ((waitArgs < 0) || (waitArgs > 2)) { throw new IllegalArgumentException(
                                                                                 "Wait argument count must be 0, 1 or 2"); }

      if (requestType != RELEASE_LOCK_REQUEST_TYPE && requestType != TRY_OBTAIN_LOCK_REQUEST_TYPE) { throw new IllegalArgumentException(
                                                                                                                                        "Can't include withWait option for non lock release requests"); }

      this.waitArgCount = waitArgs;
      this.waitMillis = millis;
      this.waitNanos = nanos;
    }
  }

}
