/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.HashMap;
import java.util.Map;

public class ClientMemoryReaperSynchronousWriteTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT    = 2;
  private static final int THREADS_COUNT = 2;

  public ClientMemoryReaperSynchronousWriteTest() {
    // workaround for MNK-671 : disabling cache manager and other logging for this test alone
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L2_CACHEMANAGER_LOGGING_ENABLED, "false");
    tcProps.setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED, "false");
    tcProps.setProperty(TCPropertiesConsts.L1_CACHEMANAGER_LOGGING_ENABLED, "false");
    tcProps.setProperty(TCPropertiesConsts.L1_TRANSACTIONMANAGER_LOGGING_ENABLED, "false");
  }

  protected Class getApplicationClass() {
    return ClientMemoryReaperTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(THREADS_COUNT);
    t.initializeTestRunner();
  }

  protected Map getOptionalAttributes() {
    Map attributes = new HashMap();
    attributes.put(ClientMemoryReaperTestApp.SYNCHRONOUS_WRITE, "true");
    return attributes;
  }

  protected void tearDown() throws Exception {
    // workaround for MNK-671 : re-enabling cache manager logging
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L2_CACHEMANAGER_LOGGING_ENABLED, "true");
    tcProps.setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED, "true");
    tcProps.setProperty(TCPropertiesConsts.L1_CACHEMANAGER_LOGGING_ENABLED, "true");
    tcProps.setProperty(TCPropertiesConsts.L1_TRANSACTIONMANAGER_LOGGING_ENABLED, "true");
    super.tearDown();
  }
}
