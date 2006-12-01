/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;


public class FooServiceImpl implements FooService {
  private int counter;
  private String prefix = "rawValue";
  
  
  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String serviceMethod() {
    return prefix + "-" + nextValue();
  }
 
  private synchronized int nextValue() {
    return counter++;
  }

}
