/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

public class AppCtxDefBean2 implements AppCtxDefBean {

  private AppCtxDefBean bean;

  public void setBean(AppCtxDefBean bean) {
    this.bean = bean;
  }
  
  public int getValue() {
    synchronized(this) {
      return this.bean.getValue();
    }
  }

  public void setValue(int value) {
    synchronized(this) {
      this.bean.setValue(value);
    }
  }
  
}
