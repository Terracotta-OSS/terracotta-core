/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.actions;

import com.tc.util.ProductInfo;

import junit.framework.TestCase;

/**
 * Test to make sure kitID() always have a value which being used in HelpHandler and AdminClientPanel classes This test
 * won't pass in Eclipse since it needs info from tcbuild
 */
public class KitIDTest extends TestCase {
  public void testKitID() throws Exception {
    System.out.println("kitID: " + ProductInfo.getInstance().kitID());
    assertTrue(ProductInfo.getInstance().kitID().matches("\\d+\\.\\d+.\\d+"));
  }
}
