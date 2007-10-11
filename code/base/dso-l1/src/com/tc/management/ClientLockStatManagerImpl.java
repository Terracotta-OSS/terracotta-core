/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.impl.TCStackTraceElement;
import com.tc.object.msg.LockStatisticsResponseMessage;
import com.tc.object.net.DSOClientMessageChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Methods in this class are not synchronized. They should be called from a proper synchronized
// context.
public class ClientLockStatManagerImpl implements ClientLockStatManager {
  private final static Set        IGNORE_STACK_TRACES_PACKAGE = new HashSet();

  private final Map               stackTracesMap              = new HashMap();
  private final Map               statEnabledLocks            = new HashMap();
  private Sink                    sink;
  private DSOClientMessageChannel channel;

  static {
    IGNORE_STACK_TRACES_PACKAGE.add("com.tc.");
    IGNORE_STACK_TRACES_PACKAGE.add("com.tcclient.");
  }

  private static LockStatisticsResponseMessage createLockStatisticsResponseMessage(ClientMessageChannel channel,
                                                                                   LockID lockID, List stackTraces) {
    LockStatisticsResponseMessage message = (LockStatisticsResponseMessage) channel
        .createMessage(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE);
    message.initialize(lockID, stackTraces);
    return message;
  }

  public void start(DSOClientMessageChannel channel, Sink sink) {
    this.channel = channel;
    this.sink = sink;
  }

  public void recordStackTrace(LockID lockID) {
    ClientLockStatContext lockStatContext = (ClientLockStatContext) statEnabledLocks.get(lockID);
    if (lockStatContext.getLockAccessedFrequency() == 0) {

      Set stackTraces = (Set) stackTracesMap.get(lockID);
      if (stackTraces == null) {
        stackTraces = new HashSet();
        stackTracesMap.put(lockID, stackTraces);
      }

      StackTraceElement[] stackTraceElements = getStackTraceElements(lockStatContext.getStackTraceDepth());
      TCStackTraceElement tcStackTraceElement = new TCStackTraceElement(stackTraceElements);

      if (!stackTraces.contains(tcStackTraceElement)) {
        stackTraces.add(tcStackTraceElement);

        List stackTracesList = new ArrayList();
        stackTracesList.add(tcStackTraceElement);
        send(lockID, stackTracesList);
      }
    }
    lockStatContext.lockAccessed();
  }

  private StackTraceElement[] getStackTraceElements(int stackTraceDepth) {
    StackTraceElement[] stackTraces = (new Exception()).getStackTrace();
    return filterStackTracesElement(stackTraces, stackTraceDepth);
  }

  private StackTraceElement[] filterStackTracesElement(StackTraceElement[] stackTraces, int stackTraceDepth) {
    LinkedList list = new LinkedList();
    int numOfStackTraceCollected = 0;
    for (int i = 0; i < stackTraces.length; i++) {
      if (shouldIgnoreClass(stackTraces[i].getClassName())) {
        continue;
      }
      list.addLast(stackTraces[i]);
      numOfStackTraceCollected++;
      if (numOfStackTraceCollected > stackTraceDepth) {
        break;
      }
    }
    StackTraceElement[] rv = new StackTraceElement[list.size()];
    return (StackTraceElement[]) list.toArray(rv);
  }

  private boolean shouldIgnoreClass(String className) {
    for (Iterator i = IGNORE_STACK_TRACES_PACKAGE.iterator(); i.hasNext();) {
      String ignorePackage = (String) i.next();
      if (className.startsWith(ignorePackage)) { return true; }
    }
    return false;
  }

  private void send(LockID lockID, List stackTraces) {
    sink.add(createLockStatisticsResponseMessage(channel.channel(), lockID, stackTraces));
  }

  public void enableStat(LockID lockID, int lockStackTraceDepth, int lockStatCollectFrequency) {
    statEnabledLocks.remove(lockID);
    ClientLockStatContext lockStatContext = new ClientLockStatContext(lockStatCollectFrequency, lockStackTraceDepth);
    statEnabledLocks.put(lockID, lockStatContext);
  }

  public void disableStat(LockID lockID) {
    statEnabledLocks.remove(lockID);
    stackTracesMap.remove(lockID);
  }

  public boolean isStatEnabled(LockID lockID) {
    return statEnabledLocks.containsKey(lockID);
  }

  private static class LRUList extends LinkedList {
    private final static int NO_LIMIT = -1;

    private int              maxSize;

    public LRUList() {
      this(NO_LIMIT);
    }

    public LRUList(int maxSize) {
      this.maxSize = maxSize;
    }

    public boolean add(Object o) {
      super.addFirst(o);
      if (maxSize != NO_LIMIT && size() > maxSize) {
        removeLast();
      }
      return true;
    }
  }

}
