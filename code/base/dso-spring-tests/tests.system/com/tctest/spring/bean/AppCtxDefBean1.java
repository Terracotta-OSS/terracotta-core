/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public class AppCtxDefBean1 implements AppCtxDefBean {

  private int value;
  private AppCtxDefBean bean;

  public int getValue() {
    synchronized(this) {
      return value;
    }
  }

  public void setValue(int value) {
    synchronized(this) {
      this.value = value;
    }
  }
  
  public void setBean(AppCtxDefBean bean) {
    this.bean = bean;
  }
  
}
