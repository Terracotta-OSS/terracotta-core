/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans.logging;

import com.tc.management.TerracottaMBean;

import javax.management.NotificationEmitter;

/**
 * MBean for manipulating client logging at runtime, as setup through the configuration.
 */

public interface RuntimeLoggingMBean extends TerracottaMBean, NotificationEmitter {
  public static final String DISTRIBUTED_METHOD_DEBUG_EVENT_TYPE = "tc.logging.runtime.DistributedMethodDebug";
  public static final String FIELD_CHANGE_DEBUG_EVENT_TYPE       = "tc.logging.runtime.FieldChangeDebug";
  public static final String LOCK_DEBUG_EVENT_TYPE               = "tc.logging.runtime.LockDebug";
  public static final String NON_PORTABLE_DUMP_EVENT_TYPE        = "tc.logging.runtime.NonPortableDump";
  public static final String WAIT_NOTIFY_DEBUG_EVENT_TYPE        = "tc.logging.runtime.WaitNotifyDebug";
  public static final String NEW_OBJECT_DEBUG_EVENT_TYPE         = "tc.logging.runtime.NewObjectDebug";
  public static final String NAMED_LOADER_DEBUG_EVENT_TYPE       = "tc.logging.runtime.NamedLoaderDebug";
  public static final String FLUSH_DEBUG_EVENT_TYPE              = "tc.logging.runtime.FlushDebug";
  public static final String FAULT_DEBUG_EVENT_TYPE              = "tc.logging.runtime.FaultDebug";

  void setNonPortableDump(boolean nonPortableDump);

  boolean getNonPortableDump();

  void setLockDebug(boolean lockDebug);

  boolean getLockDebug();

  void setFieldChangeDebug(boolean fieldChangeDebug);

  boolean getFieldChangeDebug();

  void setWaitNotifyDebug(boolean waitNotifyDebug);

  boolean getWaitNotifyDebug();

  void setDistributedMethodDebug(boolean distributedMethodDebug);

  boolean getDistributedMethodDebug();

  void setNewObjectDebug(boolean newObjectDebug);

  boolean getNewObjectDebug();

  void setNamedLoaderDebug(boolean namedLoaderDebug);

  boolean getNamedLoaderDebug();
  
  void setFlushDebug(boolean flushDebug);

  boolean getFlushDebug();

  void setFaultDebug(boolean faultDebug);

  boolean getFaultDebug();
}
