/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;


import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TcPropertiesOverWriteTest extends AbstractToolkitTestBase {

  public TcPropertiesOverWriteTest(TestConfig testConfig) throws IOException {
    super(testConfig, TcPropertiesOverWriteTestApp.class);

    testConfig.addTcProperty(TCPropertiesConsts.L1_CACHEMANAGER_ENABLED,
                             TcPropertiesOverWriteTestApp.L1_CACHEMANAGER_ENABLED_VALUE);
    testConfig.addTcProperty(TCPropertiesConsts.LOGGING_MAX_LOGFILE_SIZE,
                             TcPropertiesOverWriteTestApp.L1_LOGGING_MAX_LOGFILE_SIZE_VALUE);

    // this property is also given as a system property which has higher precedence to tc-config
    // this would not get overridden
    testConfig.addTcProperty(TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES, "2345");

    // this property is also given by tc.properties file which has higher precedence to tc-config
    // this would not get overridden
    testConfig.addTcProperty(TCPropertiesConsts.L1_CACHEMANAGER_LEASTCOUNT, "9000");

    testConfig.getClientConfig()
        .addExtraClientJvmArg("-Dcom.tc." + TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES + "="
                                  + TcPropertiesOverWriteTestApp.L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES_VALUE);

    File tcPropertiesFile = getTempFile("tc.properties");
    createTCPropertiesFile(tcPropertiesFile);
    // set the local tc.properties file location
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.properties=" + tcPropertiesFile);

  }

  private void createTCPropertiesFile(File file) {
    // properties written in this config file would get overridden by the one in tc-config
    try {
      FileWriter outFile = new FileWriter(file);
      PrintWriter out = new PrintWriter(outFile);
      out.println(TCPropertiesConsts.L1_CACHEMANAGER_LEASTCOUNT + "="
                  + TcPropertiesOverWriteTestApp.L1_CACHEMANAGER_LEASTCOUNT_VALUE);
      out.close();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

  }

}
