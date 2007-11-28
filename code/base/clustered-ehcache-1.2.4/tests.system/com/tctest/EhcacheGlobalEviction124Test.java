/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;


public class EhcacheGlobalEviction124Test extends EhcacheGlobalEvictionTestBase {

  public EhcacheGlobalEviction124Test() {
    disableAllUntil("2008-01-15");
  }
  
  protected Class getApplicationClass() {
    return EhcacheGlobalEviction124TestApp.class;
  }
}
