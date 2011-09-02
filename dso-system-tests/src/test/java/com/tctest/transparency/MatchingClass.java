/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

/**
 * @author Eugene Kuleshov
 */
public class MatchingClass {
  
  private String foo1;
  
  private transient String boo1;
  
  public String getFoo1() {
    return foo1;
  }
  
  public void setFoo1(String foo1) {
    this.foo1 = foo1;
  }
  
  public String getBoo1() {
    return boo1;
  }
  
  public void setBoo1(String boo1) {
    this.boo1 = boo1;
  }
  
}
