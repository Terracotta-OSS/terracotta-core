/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;

public class TcPropertiesWithSpacesOverWriteTest extends TCTestCase {
  private File tcConfig = null;

  public void testOverWrite() throws Exception {
      tcConfig = getTempFile("tc-config-testHaMode1.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "<tc-properties>"
                      +   "<property name=\" l1.cachemanager.enabled \" value=\"    false    \" />"
                      +   "<property name=\"   logging.maxLogFileSize  \" value=\"   1234\" />"
                      +   "<property name=\"l1.transactionmanager.maxPendingBatches   \" value=\"2345   \" />"
                      +   "<property name=\"    l1.cachemanager.leastCount    \" value=\"  9000   \" />"
                      + "</tc-properties>"
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">" 
                      + "\n      <dso-port bind=\"127.8.9.0\">6510</dso-port>"
                      + "\n      <jmx-port bind=\"127.8.9.1\">6520</jmx-port>"
                      + "\n      <l2-group-port bind=\"127.8.9.2\">6530</l2-group-port>"  
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n</servers>" 
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestConfigurationSetupManagerFactory factory = new TestConfigurationSetupManagerFactory(
                                                                                                    TestConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      
      TCProperties tcProps = TCPropertiesImpl.getProperties();
      Assert.assertEquals(false, tcProps.getBoolean("l1.cachemanager.enabled"));
      Assert.assertEquals(1234, tcProps.getInt("logging.maxLogFileSize"));
      Assert.assertEquals(2345, tcProps.getInt("l1.transactionmanager.maxPendingBatches"));
      Assert.assertEquals(9000, tcProps.getInt("l1.cachemanager.leastCount"));
    }

  private synchronized void writeConfigFile(String fileContents) {
    try {
      FileOutputStream out = new FileOutputStream(tcConfig);
      IOUtils.write(fileContents, out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }
}
