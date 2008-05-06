/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class TreeSetSimplifiedGCTest extends GCTestBase {

  public TreeSetSimplifiedGCTest() {
    // disableAllUntil(new Date(Long.MAX_VALUE));
  }
  
  protected Class getApplicationClass() {
    return TreeSetSimplifiedGCTestApp.class;
  }

}
