/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;


import java.util.List;

public class MatchingAutolockedSubclass extends MatchingClass {
  
  private String moo;
  
  public MatchingAutolockedSubclass() {
    //
  }
  
  public MatchingAutolockedSubclass(List shared) {
    synchronized(shared) {
      shared.add(this);
    }
  }
  
  public void setMoo(String moo) {
    synchronized (this) {
      this.moo = moo;
    }
  }
  
  public String getMoo() {
    return moo;
  }
  
}