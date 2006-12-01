/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aop;

public interface IDelegatingProxyTarget {
  public void doStuff(String arg); 
  public String returnStuff(String arg);
  public void throwStuff(String string) throws ExpectedException;
}
