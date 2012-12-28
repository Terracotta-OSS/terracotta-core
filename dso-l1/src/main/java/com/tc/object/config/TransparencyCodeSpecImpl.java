/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import java.util.HashSet;
import java.util.Set;

public class TransparencyCodeSpecImpl implements TransparencyCodeSpec {
  private final static Set MONITOR_INSTRUMENTATION_REQ_LOGICAL_CLASS = new HashSet();

  private boolean          arrayOperatorInstrumentationReq;
  private boolean          arraycopyInstrumentationReq;
  // private boolean fieldInstrumentationReq;
  private boolean          waitNotifyInstrumentationReq;
  private boolean          monitorInstrumentationReq;
  private boolean          forceUncheckedFieldAccess                 = false;

  public static TransparencyCodeSpec getDefaultPhysicalCodeSpec() {
    TransparencyCodeSpec defaultPhysicalCodeSpec = new TransparencyCodeSpecImpl();
    defaultPhysicalCodeSpec.setArrayOperatorInstrumentationReq(true);
    defaultPhysicalCodeSpec.setArraycopyInstrumentationReq(true);
    defaultPhysicalCodeSpec.setFieldInstrumentationReq(true);
    defaultPhysicalCodeSpec.setWaitNotifyInstrumentationReq(true);
    defaultPhysicalCodeSpec.setMonitorInstrumentationReq(true);
    return defaultPhysicalCodeSpec;
  }

  public static TransparencyCodeSpec getDefaultLogicalCodeSpec() {
    TransparencyCodeSpec defaultSpec = new TransparencyCodeSpecImpl();
    return defaultSpec;
  }

  public static TransparencyCodeSpec getDefaultCodeSpec(String className, boolean isLogical, boolean isAutolock) {
    if (isLogical) {
      TransparencyCodeSpec codeSpec = getDefaultLogicalCodeSpec();
      if (MONITOR_INSTRUMENTATION_REQ_LOGICAL_CLASS.contains(className)) {
        codeSpec.setMonitorInstrumentationReq(isAutolock);
      }
      return codeSpec;
    }
    return getDefaultPhysicalCodeSpec();
  }

  public TransparencyCodeSpecImpl() {
    super();
  }

  @Override
  public boolean isArraycopyInstrumentationReq(String className, String methodName) {
    return arraycopyInstrumentationReq && "java/lang/System".equals(className) && "arraycopy".equals(methodName);
  }

  @Override
  public void setArraycopyInstrumentationReq(boolean arraycopyInstrumentationReq) {
    this.arraycopyInstrumentationReq = arraycopyInstrumentationReq;
  }

  @Override
  public boolean isArrayOperatorInstrumentationReq() {
    return arrayOperatorInstrumentationReq;
  }

  @Override
  public void setArrayOperatorInstrumentationReq(boolean arrayOperatorInstrumentationReq) {
    this.arrayOperatorInstrumentationReq = arrayOperatorInstrumentationReq;
  }

  @Override
  public void setFieldInstrumentationReq(boolean fieldInstrumentationReq) {
    // this.fieldInstrumentationReq = fieldInstrumentationReq;
  }

  @Override
  public boolean isWaitNotifyInstrumentationReq() {
    return waitNotifyInstrumentationReq;
  }

  @Override
  public void setWaitNotifyInstrumentationReq(boolean waitNotifyInstrumentationReq) {
    this.waitNotifyInstrumentationReq = waitNotifyInstrumentationReq;
  }

  @Override
  public boolean isMonitorInstrumentationReq() {
    return monitorInstrumentationReq;
  }

  @Override
  public void setMonitorInstrumentationReq(boolean monitorInstrumentationReq) {
    this.monitorInstrumentationReq = monitorInstrumentationReq;
  }

  @Override
  public void setForceRawFieldAccess() {
    this.forceUncheckedFieldAccess = true;
  }

  @Override
  public boolean isForceRawFieldAccess() {
    return forceUncheckedFieldAccess;
  }

}
