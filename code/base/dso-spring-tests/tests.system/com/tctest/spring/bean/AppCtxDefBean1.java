/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.bean;

public class AppCtxDefBean1 implements AppCtxDefBean {

  private int           value;
  private AppCtxDefBean bean;

  public synchronized int getValue() {
    return value;
  }

  public synchronized void setValue(int value) {
    this.value = value;
  }

  public synchronized void setBean(AppCtxDefBean bean) {
    this.bean = bean;
  }

  public synchronized AppCtxDefBean getBean() {
    return bean;
  }

}
