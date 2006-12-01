/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public class BeanWithNamedLock implements IActiveBean, Runnable {
  private String            value = "0";

  private transient boolean started;
  private transient String  localValue;
  private transient boolean active;

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.localValue = value;
  }

  public boolean isActive() {
    return active;
  }
  
  public void start() {
    this.started = true;

    Thread thread = new Thread(this);
    thread.start();
  }

  public void stop() {
    this.started = false;
  }

  public void run() {
    try {
      while (this.started) {
        this.active = true;
        if (this.localValue != null) {
          this.value = this.localValue;
          this.localValue = null;
        }
  
        try {
          Thread.sleep(100L);
        } catch (InterruptedException e) {
          // ignore
        }
      }
    } finally {
      this.active = false;
    }
  }

}
