/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;


public class FooServiceImpl implements FooService {
  private int counter;
  private String prefix = "rawValue";
  
  
  public synchronized void setPrefix(String prefix) {
    this.prefix = prefix;
  }
  
  public synchronized String getPrefix() {
    return prefix;
  }

  public synchronized String serviceMethod() {
    return getPrefix() + "-" + nextValue();
  }
 
  private synchronized int nextValue() {
    return counter++;
  }
  
  public synchronized int getCounter() {
    return counter;
  }

}
