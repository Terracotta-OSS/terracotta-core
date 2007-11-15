/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.properties.TCPropertiesImpl;

public class ClientMemoryReaperTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT        = 2;
  private static final int THREADS_COUNT     = 2;

  public ClientMemoryReaperTest() {
    // MNK-405
    //disableAllUntil("2007-11-20");
   
    //workaround for MNK-405 : disabling cache manager and other logging for this test alone
    TCPropertiesImpl.setProperty("l2.cachemanager.logging.enabled", "false");
    TCPropertiesImpl.setProperty("l2.objectmanager.fault.logging.enabled", "false");
    TCPropertiesImpl.setProperty("l1.cachemanager.logging.enabled", "false");
    TCPropertiesImpl.setProperty("l1.transactionmanager.logging.enabled", "false");
  }
  
  protected Class getApplicationClass() {
    return ClientMemoryReaperTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(THREADS_COUNT);
    t.initializeTestRunner();
  }
  
  protected void tearDown() throws Exception {
    //workaround for MNK-405 : re-enabling cache manager logging
    TCPropertiesImpl.setProperty("l2.cachemanager.logging.enabled", "true");
    TCPropertiesImpl.setProperty("l2.objectmanager.fault.logging.enabled", "true");
    TCPropertiesImpl.setProperty("l1.cachemanager.logging.enabled", "true");
    TCPropertiesImpl.setProperty("l1.transactionmanager.logging.enabled", "true");
    super.tearDown();
  }

}
