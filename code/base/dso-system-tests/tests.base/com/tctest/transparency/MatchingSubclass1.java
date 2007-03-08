/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

/**
 * @author Eugene Kuleshov
 */
public class MatchingSubclass1 extends MatchingClass {

  private String foo;
  
  public String getFoo() {
    return foo;
  }
  
  public void setFoo(String foo) {
    this.foo = foo;
  }
  
}
