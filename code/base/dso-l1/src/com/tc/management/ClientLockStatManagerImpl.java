/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.async.api.Sink;
import com.tc.exception.TCRuntimeException;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.impl.TCStackTraceElement;
import com.tc.object.msg.LockStatisticsResponseMessage;
import com.tc.object.net.DSOClientMessageChannel;

import java.lang.reflect.Field;
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
    if (lockStatContext.shouldRecordStackTrace()) {

      Set stackTraces = (Set) stackTracesMap.get(lockID);
      if (stackTraces == null) {
        stackTraces = new HashSet();
        stackTracesMap.put(lockID, stackTraces);
      }

      StackTraceElement[] stackTraceElements = getStackTraceElements(lockStatContext.getStackTraceDepth());
      TCStackTraceElement tcStackTraceElement = new TCStackTraceElement(stackTraceElements);

      boolean added = stackTraces.add(tcStackTraceElement);

      if (added) {
        List stackTracesList = new ArrayList();
        stackTracesList.add(tcStackTraceElement);
        send(lockID, stackTracesList);
      }
    }
    lockStatContext.updateCollectTimer();
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

  private void send(LockID lockID, List stackTraces) {
    sink.add(createLockStatisticsResponseMessage(channel.channel(), lockID, stackTraces));
  }

  public void enableStackTrace(LockID lockID, int lockStackTraceDepth, int lockStatCollectFrequency) {
    statEnabledLocks.remove(lockID);
    stackTracesMap.remove(lockID);

    ClientLockStatContext lockStatContext = new ClientLockStatContext(lockStatCollectFrequency, lockStackTraceDepth);
    statEnabledLocks.put(lockID, lockStatContext);
  }

  public void disableStackTrace(LockID lockID) {
    statEnabledLocks.remove(lockID);
    stackTracesMap.remove(lockID);
  }

  public boolean isStatEnabled(LockID lockID) {
    return statEnabledLocks.containsKey(lockID);
  }
}
