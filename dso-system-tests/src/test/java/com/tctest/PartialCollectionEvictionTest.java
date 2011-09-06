/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

public class PartialCollectionEvictionTest extends TransparentTestBase {

  private final static int NODE_COUNT = 1;

  public void setUp() throws Exception {
    super.setUp();
    
    getTransparentAppConfig().setClientCount(NODE_COUNT);
    initializeTestRunner();
  }
  
  @Override
  protected Class getApplicationClass() {
    return PartialCollectionEvictionTestApp.class;
  }

}
