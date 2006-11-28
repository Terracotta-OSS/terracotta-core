/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
