/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;


import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

/**
 * See DEV-2912, CDV-1374
 */
public class TxnFoldNewObjectNewTypeTest extends AbstractToolkitTestBase {

  public TxnFoldNewObjectNewTypeTest(TestConfig testConfig) {
    super(testConfig, TxnFoldNewObjectNewTypeTestClient.class, TxnFoldNewObjectNewTypeTestClient.class);
    testConfig.addTcProperty(TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXOUTSTANDING_BATCHSIZE, "1");
  }

}
