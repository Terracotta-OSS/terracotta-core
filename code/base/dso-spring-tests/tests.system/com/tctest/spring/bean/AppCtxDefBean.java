/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;


public interface AppCtxDefBean {

  void setValue(int value);
  
  int getValue();
  
  void setBean(AppCtxDefBean bean);
  
}
