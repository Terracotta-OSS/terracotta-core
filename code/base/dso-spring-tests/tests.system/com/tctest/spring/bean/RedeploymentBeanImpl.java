/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import java.io.InputStream;

public class RedeploymentBeanImpl implements RedeploymentBean {

  private int value;

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

  public boolean hasResource(String resource) {
    InputStream is;
    try {
      is = getClass().getResourceAsStream(resource);
      return is!=null;
    } catch (Exception e) {
      return false;
    }
  }
  
}
