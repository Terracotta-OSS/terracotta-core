/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class ConcurrentHashMapGCTest extends GCTestBase implements TestConfigurator {
  public ConcurrentHashMapGCTest() {
    disableAllUntil("2007-01-15");
  }
  
  protected Class getApplicationClass() {
    return ConcurrentHashMapSwapingTestApp.class;
  }

}
