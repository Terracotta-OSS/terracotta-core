/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

/**
 * Transparency code specification, modifies method code
 */
public interface TransparencyCodeSpec {

  /**
   * Check whether calling System.arraycopy() should be instrumented
   *
   * @param className Class name to check
   * @prarm methodName Method to check
   * @return True to instrument
   */
  public boolean isArraycopyInstrumentationReq(String className, String methodName);

  /**
   * Set that System.arraycopy() should be instrumented
   *
   * @param arraycopyInstrumentationReq Flag
   */
  public void setArraycopyInstrumentationReq(boolean arraycopyInstrumentationReq);

  /**
   * Check whether store operations on arrays should be instrumented
   *
   * @return True to instrument
   */
  public boolean isArrayOperatorInstrumentationReq();

  /**
   * Set that array store operations should be instrumented
   *
   * @param arrayOperatorInstrumentationReq Flag
   */
  public void setArrayOperatorInstrumentationReq(boolean arrayOperatorInstrumentationReq);

  /**
   * Check whether field access should be instrumented
   *
   * @param fieldName Field to check
   * @return True to instrument
   */
  public boolean isFieldInstrumentationReq(String fieldName);

  /**
   * Set that field access should be instrumented
   *
   * @param fieldInstrumentationReq Flag
   */
  public void setFieldInstrumentationReq(boolean fieldInstrumentationReq);

  /**
   * Check whether wait/notify instructions should be instrumented
   *
   * @return True to instrument
   */
  public boolean isWaitNotifyInstrumentationReq();

  /**
   * Set that wait/notify should be instrumented
   *
   * @param waitNotifyInstrumentationReq Flag
   */
  public void setWaitNotifyInstrumentationReq(boolean waitNotifyInstrumentationReq);

  /**
   * Check whether MONITORENTER and MONNITOREXIT should be instrumented
   *
   * @return True if MONITORENTER and MONNITOREXIT should be instrumented
   */
  public boolean isMonitorInstrumentationReq();

  /**
   * Set that MONITORENTER and MONNITOREXIT should be instrumented
   *
   * @param monitorInstrumentationReq Flag
   */
  public void setMonitorInstrumentationReq(boolean monitorInstrumentationReq);

  /**
   * Force the code adapter to use raw field reads
   */
  public void setForceRawFieldAccess();

  /**
   * Should field accessed be forced to be raw access
   */
  public boolean isForceRawFieldAccess();

}
