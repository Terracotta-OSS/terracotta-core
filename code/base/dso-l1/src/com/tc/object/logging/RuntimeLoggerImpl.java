/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.logging;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.TCObject;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.locks.DsoLiteralLockID;
import com.tc.object.locks.DsoLockID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.tx.TimerSpec;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.util.NullStatsRecorder;
import com.tc.statistics.util.StatsPrinter;
import com.tc.statistics.util.StatsRecorder;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class RuntimeLoggerImpl implements RuntimeLogger {
  private final TCLogger             logger;
  private final StatsRecorderManager statsRecorderManager = new StatsRecorderManager();

  private boolean                    lockDebug;
  private boolean                    fieldChangeDebug;
  private boolean                    arrayChangeDebug;
  private boolean                    newManagedObjectDebug;
  private boolean                    distributedMethodDebug;
  private boolean                    nonPortableDump;
  private boolean                    waitNotifyDebug;

  private boolean                    fullStack;
  private boolean                    autoLockDetails;

  private final String               flushDebugStats      = "flushStats";
  private final String               faultDebugStats      = "faultStats";

  private boolean                    namedLoaderDebug;

  public RuntimeLoggerImpl(DSOClientConfigHelper configHelper) {
    this.logger = CustomerLogging.getDSORuntimeLogger();

    // runtime logging items
    this.lockDebug = configHelper.runtimeLoggingOptions().logLockDebug();
    this.fieldChangeDebug = configHelper.runtimeLoggingOptions().logFieldChangeDebug();
    this.arrayChangeDebug = fieldChangeDebug;
    this.newManagedObjectDebug = configHelper.runtimeLoggingOptions().logNewObjectDebug();
    this.distributedMethodDebug = configHelper.runtimeLoggingOptions().logDistributedMethodDebug();
    this.nonPortableDump = configHelper.runtimeLoggingOptions().logNonPortableDump();
    this.waitNotifyDebug = configHelper.runtimeLoggingOptions().logWaitNotifyDebug();
    this.namedLoaderDebug = configHelper.runtimeLoggingOptions().logNamedLoaderDebug();

    // runtime logging options
    this.fullStack = configHelper.runtimeOutputOptions().doFullStack();
    this.autoLockDetails = configHelper.runtimeOutputOptions().doAutoLockDetails();

    setFlushDebug(TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_OBJECTMANAGER_FLUSH_LOGGING_ENABLED));
    setFaultDebug(TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_OBJECTMANAGER_FAULT_LOGGING_ENABLED));

  }

  public void setLockDebug(boolean lockDebug) {
    this.lockDebug = lockDebug;
  }

  public boolean getLockDebug() {
    return this.lockDebug;
  }

  public void setFieldChangeDebug(boolean fieldChangeDebug) {
    this.fieldChangeDebug = fieldChangeDebug;
  }

  public boolean getFieldChangeDebug() {
    return this.fieldChangeDebug;
  }

  public void setArrayChangeDebug(boolean arrayChangeDebug) {
    this.arrayChangeDebug = arrayChangeDebug;
  }

  public boolean getArrayChangeDebug() {
    return this.arrayChangeDebug;
  }

  public void setNewManagedObjectDebug(boolean newManagedObjectDebug) {
    this.newManagedObjectDebug = newManagedObjectDebug;
  }

  public boolean getNewManagedObjectDebug() {
    return this.newManagedObjectDebug;
  }

  public void setWaitNotifyDebug(boolean waitNotifyDebug) {
    this.waitNotifyDebug = waitNotifyDebug;
  }

  public boolean getWaitNotifyDebug() {
    return this.waitNotifyDebug;
  }

  public void setDistributedMethodDebug(boolean distributedMethodDebug) {
    this.distributedMethodDebug = distributedMethodDebug;
  }

  public boolean getDistributedMethodDebug() {
    return this.distributedMethodDebug;
  }

  public void setNonPortableDump(boolean nonPortableDump) {
    this.nonPortableDump = nonPortableDump;
  }

  public boolean getNonPortableDump() {
    return this.nonPortableDump;
  }

  public void setFullStack(boolean fullStack) {
    this.fullStack = fullStack;
  }

  public boolean getFullStack() {
    return this.fullStack;
  }

  public void setCaller(boolean caller) {
    // deprecated (see CDV-731, CDV-815)
  }

  public boolean getCaller() {
    // deprecated (see CDV-731, CDV-815)
    return false;
  }

  public void setAutoLockDetails(boolean autoLockDetails) {
    this.autoLockDetails = autoLockDetails;
  }

  public boolean getAutoLockDetails() {
    return this.autoLockDetails;
  }

  public void setFlushDebug(boolean flushDebug) {
    this.statsRecorderManager.getAndSetStatsRecorder(this.flushDebugStats, flushDebug,
                                                     new MessageFormat("ManagedObjects flushed in the Last {0} ms"),
                                                     new MessageFormat(" {0} instances"), true);
  }

  public boolean getFlushDebug() {
    return this.statsRecorderManager.isDebugEnabled(this.flushDebugStats);
  }

  public void updateFlushStats(String type) {
    StatsRecorder flushStatsRecorder = this.statsRecorderManager.get(this.flushDebugStats);
    flushStatsRecorder.updateStats(type, StatsRecorder.SINGLE_INCR);
  }

  public void setFaultDebug(boolean faultDebug) {
    this.statsRecorderManager.getAndSetStatsRecorder(this.flushDebugStats, faultDebug,
                                                     new MessageFormat("ManagedObjects faulted in the Last {0} ms"),
                                                     new MessageFormat(" {0} instances"), true);
  }

  public boolean getFaultDebug() {
    return this.statsRecorderManager.isDebugEnabled(this.faultDebugStats);
  }

  public void setNamedLoaderDebug(boolean value) {
    this.namedLoaderDebug = value;
  }

  public boolean getNamedLoaderDebug() {
    return this.namedLoaderDebug;
  }

  public void updateFaultStats(String type) {
    StatsRecorder faultStatsRecorder = this.statsRecorderManager.get(this.faultDebugStats);
    faultStatsRecorder.updateStats(type, StatsRecorder.SINGLE_INCR);
  }

  public void lockAcquired(LockID lock, LockLevel level) {
    StringBuffer message = new StringBuffer("Lock [").append(lock).append("] acquired with level ").append(level);

    if (autoLockDetails && (lock instanceof DsoLockID || lock instanceof DsoLiteralLockID)) {
      message.append("\n  AUTOLOCK DETAILS NOT CURRENTLY SUPPORTED: ");
    }

    appendCall(message);
    logger.info(message);

  }

  private void appendCall(StringBuffer message) {
    if (fullStack) {
      StackTraceElement[] stack = new Throwable().getStackTrace();
      if (stack != null) {
        message.append("\n");
        for (int i = 0; i < stack.length; i++) {
          message.append("  at ").append(stack[i].toString());

          if (i < (stack.length - 1)) {
            message.append("\n");
          }
        }
      }
    }

  }

  public void literalValueChanged(TCObject source, Object newValue) {
    StringBuffer message = new StringBuffer("DSO object literal value changed\n");
    if (newValue != null) {
      message.append("\n  newValue type: ").append(newValue.getClass().getName());
      message.append(", identityHashCode: 0x").append(Integer.toHexString(System.identityHashCode(newValue)));
    } else {
      message.append("\n  newValue: null");
    }

    logger.info(message.toString());
  }

  public void fieldChanged(TCObject source, String classname, String fieldName, Object newValue, int index) {
    StringBuffer message = new StringBuffer("DSO object field changed\n");
    message.append("  class: ").append(classname).append(", field: ").append(fieldName);
    if (index >= 0) {
      message.append(", index: ").append(index);
    }
    if (newValue != null) {
      message.append("\n  newValue type: ").append(newValue.getClass().getName());
      message.append(", identityHashCode: 0x").append(Integer.toHexString(System.identityHashCode(newValue)));
    } else {
      message.append("\n  newValue: null");
    }

    logger.info(message.toString());
  }

  public void arrayChanged(TCObject source, int startPos, Object array) {
    StringBuffer message = new StringBuffer("DSO array changed\n");
    message.append("\n startPos: ").append(startPos);
    message.append("\n subset component types: \n").append(array.getClass().getComponentType());

    logger.info(message.toString());
  }

  public void newManagedObject(TCObject object) {
    StringBuffer message = new StringBuffer("New DSO Object instance created\n");
    message.append("  instance: ").append(baseToString(object.getPeerObject())).append("\n");
    message.append("  object ID: ").append(object.getObjectID());
    appendCall(message);
    logger.info(message.toString());
  }

  public void objectNotify(boolean all, Object obj, TCObject tcObject) {
    StringBuffer message = new StringBuffer("notify").append(all ? "All()" : "()");
    message.append(" called on ").append(baseToString(obj)).append(", ObjectID: ").append(
                                                                                          tcObject.getObjectID()
                                                                                              .toLong());
    logger.info(message.toString());
  }

  public void objectWait(TimerSpec call, Object obj, TCObject tcObject) {
    StringBuffer message = new StringBuffer(call.toString()).append(" called on ");
    message.append(baseToString(obj)).append(", ObjectID: ").append(tcObject.getObjectID().toLong());
    logger.info(message.toString());
  }

  public void distributedMethodCall(String receiverName, String methodName, String params) {
    StringBuffer message = new StringBuffer("Distributed method invoked\n");
    message.append("  receiver class: ").append(receiverName).append("\n");
    message.append("  methodName: ").append(methodName).append("\n");
    message.append("  params: ").append(params).append("\n");

    appendCall(message);
    logger.info(message.toString());
  }

  public void distributedMethodCallError(String receiverClassName, String methodName, String params, Throwable error) {
    StringBuffer message = new StringBuffer("Unhandled execption occurred in distributed method call\n");
    message.append(" receiver class: ").append(receiverClassName).append("\n");
    message.append(" methodName: ").append(methodName).append("\n");
    message.append(" params: ").append(params).append("\n");

    logger.warn(message.toString(), error);
  }

  private static String baseToString(Object obj) {
    if (obj == null) { return null; }
    return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
  }

  public void namedLoaderRegistered(NamedClassLoader loader, String name, String appGroup, NamedClassLoader previous) {
    StringBuffer message = new StringBuffer("loader of type [");
    message.append(loader.getClass().getName()).append("] with name [").append(name);
    if (appGroup != null) {
      message.append("] in app group [").append(appGroup);
    }
    message.append("] registered (replaced: ").append(previous != null).append(")");

    appendCall(message);
    logger.info(message);
  }

  public void shutdown() {
    this.statsRecorderManager.shutdown();
  }

  private static class StatsRecorderManager {
    private final Map<String, StatsRecorder> recorders    = new HashMap<String, StatsRecorder>();
    private final StatsRecorder              nullRecorder = new NullStatsRecorder();

    public synchronized StatsRecorder getAndSetStatsRecorder(String name, boolean isDebug, MessageFormat header,
                                                             MessageFormat formatLine, boolean printTotal) {
      StatsRecorder recorder = get(name);
      recorder.finish();
      recorder = isDebug ? createStatsPrinter(header, formatLine, printTotal) : this.nullRecorder;
      recorders.put(name, recorder);
      return recorder;
    }

    public synchronized StatsRecorder get(String name) {
      StatsRecorder recorder = recorders.get(name);
      if (recorder == null) {
        recorder = this.nullRecorder;
        recorders.put(name, recorder);
      }
      return recorder;
    }

    public synchronized boolean isDebugEnabled(String name) {
      return get(name) != this.nullRecorder;
    }

    private StatsRecorder createStatsPrinter(MessageFormat header, MessageFormat formatLine, boolean printTotal) {
      StatsRecorder statsRecorder = new StatsPrinter(header, formatLine, printTotal);
      return statsRecorder;
    }

    public synchronized void shutdown() {
      for (StatsRecorder recorder : recorders.values()) {
        recorder.finish();
      }
      recorders.clear();
    }
  }

}
