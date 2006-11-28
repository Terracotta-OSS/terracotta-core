/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.aop;

public class DelegatingProxyTarget implements IDelegatingProxyTarget {
  public void doStuff(String arg) {
    Logger.log += "doStuff ";
  }

  public String returnStuff(String arg) {
    Logger.log += "returnStuff ";
    return "stuff";
  }


  public void throwStuff(String string) throws ExpectedException {
    Logger.log += "throwStuff ";
    throw new ExpectedException("expected");
  }
}