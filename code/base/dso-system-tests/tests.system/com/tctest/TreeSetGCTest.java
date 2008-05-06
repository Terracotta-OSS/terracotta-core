/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class TreeSetGCTest extends GCTestBase {

  public TreeSetGCTest() {
    // disableAllUntil(new Date(Long.MAX_VALUE));
  }
  
  protected Class getApplicationClass() {
    return TreeSetGCTestApp.class;
  }

}
