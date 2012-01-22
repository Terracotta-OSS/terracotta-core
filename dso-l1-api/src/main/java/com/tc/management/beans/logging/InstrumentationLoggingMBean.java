/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.logging;

import com.tc.management.TerracottaMBean;

import javax.management.NotificationEmitter;

public interface InstrumentationLoggingMBean extends TerracottaMBean, NotificationEmitter {
  public static final String CLASS_EVENT_TYPE               = "tc.logging.instrumentation.Class1";
  public static final String DISTRIBUTED_METHODS_EVENT_TYPE = "tc.logging.instrumentation.DistributedMethods";
  public static final String LOCKS_EVENT_TYPE               = "tc.logging.instrumentation.Locks";
  public static final String ROOTS_EVENT_TYPE               = "tc.logging.instrumentation.Roots";
  public static final String TRANSIENT_ROOT_EVENT_TYPE      = "tc.logging.instrumentation.TransientRoot";

  void setClass1(boolean clas);

  boolean getClass1();

  void setHierarchy(boolean hierarchy);

  boolean getHierarchy();

  void setLocks(boolean locks);

  boolean getLocks();

  void setTransientRoot(boolean transientRoot);

  boolean getTransientRoot();

  void setRoots(boolean roots);

  boolean getRoots();

  void setDistributedMethods(boolean distributedMethod);

  boolean getDistributedMethods();
}
