/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.async.api.Sink;
import com.tc.exception.TCRuntimeException;
import com.tc.management.ClientLockStatManager;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDImpl;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.net.DSOClientMessageChannel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

// Methods in this class are not synchronized. They should be called from a proper synchronized
// context, which is from the ClientLockManager context.
public class ClientLockStatisticsManagerImpl extends LockStatisticsManager implements ClientLockStatManager {
  private final static NodeID     NULL_NODE_ID                = NodeIDImpl.NULL_ID;
  private final static Set        IGNORE_STACK_TRACES_PACKAGE = new HashSet();

  private Sink                    sink;
  private DSOClientMessageChannel channel;

  static {
    IGNORE_STACK_TRACES_PACKAGE.add("com.tc.");
    IGNORE_STACK_TRACES_PACKAGE.add("com.tcclient.");
  }

  private static LockStatisticsResponseMessage createLockStatisticsResponseMessage(ClientMessageChannel channel,
                                                                                   Collection allTCLockStatElements) {
    LockStatisticsResponseMessage message = (LockStatisticsResponseMessage) channel
        .createMessage(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE);
    message.initialize(allTCLockStatElements);
    return message;
  }

  public void start(DSOClientMessageChannel clientMessageChannel, Sink sinkArg) {
    this.channel = clientMessageChannel;
    this.sink = sinkArg;
  }

  public synchronized void recordLockRequested(LockID lockID, ThreadID threadID, String contextInfo,
                                               int numberOfPendingLockRequests) {
    if (!isClientStatEnabled()) { return; }

    StackTraceElement[] stackTraceElements = getStackTraceElements(lockStatConfig.getTraceDepth());
    super.recordLockRequested(lockID, NULL_NODE_ID, threadID, stackTraceElements, contextInfo,
                              numberOfPendingLockRequests);
  }

  public synchronized void recordLockAwarded(LockID lockID, ThreadID threadID) {
    if (!isClientStatEnabled()) { return; }

    int depth = super.incrementNestedDepth(threadID);
    super.recordLockAwarded(lockID, NULL_NODE_ID, threadID, false, System.currentTimeMillis(), depth);
  }

  public synchronized void recordLockReleased(LockID lockID, ThreadID threadID) {
    if (!isClientStatEnabled()) { return; }

    super.decrementNestedDepth(threadID);
    super.recordLockReleased(lockID, NULL_NODE_ID, threadID);
  }

  public synchronized void recordLockHopped(LockID lockID, ThreadID threadID) {
    if (!isClientStatEnabled()) { return; }

    StackTraceElement[] stackTraceElements = getStackTraceElements(lockStatConfig.getTraceDepth());

    ClientLockStatisticsInfoImpl lsc = (ClientLockStatisticsInfoImpl) getLockStatInfo(lockID);
    if (lsc != null) {
      lsc.recordLockHopRequested(NULL_NODE_ID, threadID, stackTraceElements);
    }
  }

  public synchronized void recordLockRejected(LockID lockID, ThreadID threadID) {
    if (!isClientStatEnabled()) { return; }

    super.recordLockRejected(lockID, NULL_NODE_ID, threadID);
  }

  public synchronized void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    disableLockStatistics();
    super.setLockStatisticsEnabled(true);
    super.setLockStatisticsConfig(traceDepth, gatherInterval);
  }

  protected LockStatisticsInfo newLockStatisticsContext(LockID lockID) {
    return new ClientLockStatisticsInfoImpl(lockID, lockStatConfig.getGatherInterval());
  }

  protected void disableLockStatistics() {
    super.clear();
  }

  private boolean isClientStatEnabled() {
    if (!lockStatisticsEnabled) { return false; }

    return lockStatConfig.getTraceDepth() > MIN_CLIENT_TRACE_DEPTH;
  }

  private Collection getLockStatElements(LockID lockID) {
    LockStatisticsInfo lsc = getLockStatInfo(lockID);
    if (lsc == null) { return Collections.EMPTY_LIST; }

    lsc.aggregateLockHoldersData();
    return lsc.children();
  }

  private StackTraceElement[] getStackTraceElements(int stackTraceDepth) {
    StackTraceElement[] stackTraces = (new Exception()).getStackTrace();
    return filterStackTracesElement(stackTraces, stackTraceDepth);
  }

  private StackTraceElement[] filterStackTracesElement(StackTraceElement[] stackTraces, int stackTraceDepth) {
    stackTraces = fixTCInstrumentationStackTraces(stackTraces);

    List list = new ArrayList();
    int numOfStackTraceCollected = 0;
    for (int i = 0; i < stackTraces.length; i++) {
      if (shouldIgnoreClass(stackTraces[i].getClassName())) {
        continue;
      }
      list.add(stackTraces[i]);
      numOfStackTraceCollected++;
      if (numOfStackTraceCollected >= stackTraceDepth) {
        break;
      }
    }
    StackTraceElement[] rv = new StackTraceElement[list.size()];
    return (StackTraceElement[]) list.toArray(rv);
  }

  private StackTraceElement[] fixTCInstrumentationStackTraces(StackTraceElement[] stackTraces) {
    LinkedList list = new LinkedList();
    for (int i = 0; i < stackTraces.length; i++) {
      if (isTCInstrumentationStackTrace(stackTraces, i)) {
        setStackTraceLineNumber(stackTraces[i + 1], stackTraces[i].getLineNumber());
        list.addLast(stackTraces[i + 1]);
        i++;
      } else {
        list.addLast(stackTraces[i]);
      }
    }
    StackTraceElement[] rv = new StackTraceElement[list.size()];
    return (StackTraceElement[]) list.toArray(rv);
  }

  private boolean isTCInstrumentationStackTrace(StackTraceElement[] stackTraces, int index) {
    if (stackTraces[index].getMethodName().startsWith(ByteCodeUtil.TC_METHOD_PREFIX)) {
      if (!stackTraces[index + 1].getMethodName().startsWith(ByteCodeUtil.TC_METHOD_PREFIX)) {
        if (stackTraces[index].getMethodName().endsWith(stackTraces[index + 1].getMethodName())) { return true; }
      }
    }
    return false;
  }

  private void setStackTraceLineNumber(StackTraceElement se, int newLineNumber) {
    try {
      Field f = StackTraceElement.class.getDeclaredField("lineNumber");
      f.setAccessible(true);
      f.set(se, new Integer(newLineNumber));
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
  }

  private boolean shouldIgnoreClass(String className) {
    for (Iterator i = IGNORE_STACK_TRACES_PACKAGE.iterator(); i.hasNext();) {
      String ignorePackage = (String) i.next();
      if (className.startsWith(ignorePackage)) { return true; }
    }
    return false;
  }

  public void requestLockSpecs() {
    Collection allTCLockStatElements = new ArrayList();

    synchronized (this) {
      Set allLockIDs = lockStats.keySet();
      for (Iterator i = allLockIDs.iterator(); i.hasNext();) {
        LockID lockID = (LockID) i.next();
        Collection lockStatElements = getLockStatElements(lockID);

        allTCLockStatElements.add(new TCStackTraceElement(lockID, lockStatElements));
      }
    }
    sink.add(createLockStatisticsResponseMessage(channel.channel(), allTCLockStatElements));
  }
}
