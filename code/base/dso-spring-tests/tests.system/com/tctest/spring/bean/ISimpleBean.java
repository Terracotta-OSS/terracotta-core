/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public interface ISimpleBean {

  public long getSharedRefId();
  
  public ISimpleBean getSharedRef();

  public void setSharedRef(ISimpleBean child);

  public String getDsoTransientField();

  public void setDsoTransientField(String dsoTransientField);

  public String getField();

  public void setField(String field);

  public String getStaticField();

  public void setStaticField(String field);

  public ISimpleBean getTransientRef();

  public void setTransientRef(ISimpleBean transientChild);

  public String getTransientField();

  public void setTransientField(String transientField);

  public int getInstanceCnt();

  public int getHashCode();

  public long getId();

  public long getSharedId();

  public void setSharedId(long sharedId);
  
  public long getTimeStamp();
  
  public String getBeanName();
 
}