/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;


public interface AppCtxDefBean {

  void setValue(int value);
  
  int getValue();
  
  void setBean(AppCtxDefBean bean);
  
}
