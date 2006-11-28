/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class WorkQueuesTest1 extends TransparentTestBase
{
    public void doSetUp(TransparentTestIface t) throws Exception 
    {
     throw new Exception("This test requires at least one reader");
    }
    
    protected Class getApplicationClass()
    {
      return WorkQueuesTestApp.class;
    }
}
