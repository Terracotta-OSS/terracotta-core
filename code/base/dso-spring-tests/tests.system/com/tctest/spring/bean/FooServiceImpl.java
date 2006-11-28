/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
