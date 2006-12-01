/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public class BeanWithWaitNotify implements IActiveBean, Runnable {
  private int               value = 0;
  private Object            mutex = new Object();

  private transient boolean started;

  public String getValue() {
    synchronized (this) {
      return "" + value;
    }
  }

  public void setValue(String value) {
    synchronized (mutex) {
      mutex.notifyAll();
    }
  }

  public boolean isActive() {
    return started;
  }
  
  public void start() {
    started = true;

    Thread thread = new Thread(this);
    thread.start();
  }

  public void stop() {
    started = false;
    synchronized (mutex) {
      mutex.notifyAll();
    }
  }

  public void run() {
    while (started) {
      synchronized (mutex) {
        try {
          mutex.wait();
          value++;
        } catch (InterruptedException e) {
          //
        }
      }
    }
  }

}
