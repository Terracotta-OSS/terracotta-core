/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.bean;

public class AppCtxDefBean2 implements AppCtxDefBean {

  private AppCtxDefBean bean;

  public synchronized void setBean(AppCtxDefBean bean) {
    this.bean = bean;
  }

  public synchronized int getValue() {
    return this.bean.getValue();
  }

  public synchronized void setValue(int value) {
    this.bean.setValue(value);
  }

}
