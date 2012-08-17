/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.AbstractExpressActivePassiveTest;

import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

public class ObjectDataRapidL2DisconnectActivePassiveTest extends AbstractExpressActivePassiveTest {

  public ObjectDataRapidL2DisconnectActivePassiveTest(TestConfig testConfig) {
    super(testConfig, ObjectDataRapidL2DisconnectActivePassiveTestApp.class);
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.seda." + ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE
                                                      + ".sleepMs=100");
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc."
                                                      + TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE
                                                      + "=1");
  }

}
