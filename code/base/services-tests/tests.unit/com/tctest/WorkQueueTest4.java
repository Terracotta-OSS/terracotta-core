/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;



public class WorkQueueTest4 extends TransparentTestBase
{
  private static final int NODE_COUNT = 4;

  public void doSetUp(TransparentTestIface t) throws Exception 
  {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }
  
  protected Class getApplicationClass()
  {
    return WorkQueueTestApp.class;
  }
}