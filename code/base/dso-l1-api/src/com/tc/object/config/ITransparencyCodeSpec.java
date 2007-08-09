/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

public interface ITransparencyCodeSpec {

  public boolean isArraycopyInstrumentationReq(String className, String methodName);

  public void setArraycopyInstrumentationReq(boolean arraycopyInstrumentationReq);

  public boolean isArrayOperatorInstrumentationReq();

  public void setArrayOperatorInstrumentationReq(boolean arrayOperatorInstrumentationReq);

  public boolean isFieldInstrumentationReq(String fieldName);

  public void setFieldInstrumentationReq(boolean fieldInstrumentationReq);

  public boolean isWaitNotifyInstrumentationReq();

  public void setWaitNotifyInstrumentationReq(boolean waitNotifyInstrumentationReq);

  public boolean isMonitorInstrumentationReq();

  public void setMonitorInstrumentationReq(boolean monitorInstrumentationReq);

}
