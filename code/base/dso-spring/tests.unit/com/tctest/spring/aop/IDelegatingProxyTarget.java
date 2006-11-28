/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.aop;

public interface IDelegatingProxyTarget {
  public void doStuff(String arg); 
  public String returnStuff(String arg);
  public void throwStuff(String string) throws ExpectedException;
}
