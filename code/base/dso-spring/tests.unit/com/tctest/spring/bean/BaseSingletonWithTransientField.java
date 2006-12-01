/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public class BaseSingletonWithTransientField {

  protected transient boolean transientValue;

  public boolean isTransientValue() {
    return transientValue;
  }

  public BaseSingletonWithTransientField() {
    this.transientValue = true;
  }

  public void setTransientValue(boolean transientValue) {
    this.transientValue = transientValue;
  }

}
