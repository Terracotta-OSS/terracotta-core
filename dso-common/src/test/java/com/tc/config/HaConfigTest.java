/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;

public class HaConfigTest extends TCTestCase {
  private File tcConfig = null;

  public void testBasicMakeAllNodes() {
    try {
      tcConfig = getTempFile("tc-config-testFakeL2sName.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "\n<servers>"
                      + "\n      <server name=\"server1\" />" + "\n</servers>" + "\n</tc:tc-config>";
      writeConfigFile(config);

      // test for picking up default active server group
      ConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(
                                                                                                    new String[] {
                                                                                                        "-f",
                                                                                                        tcConfig
                                                                                                            .getAbsolutePath() },
                                                                                                    StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                                                    new FatalIllegalConfigurationChangeHandler(), null);
      HaConfig haConfig = new HaConfigImpl(factory.createL2TVSConfigurationSetupManager(null));
      Assert.assertTrue(haConfig.getNodesStore().getAllNodes().length == 1);

      // test for picking up right active server group for a give server
      factory = new StandardConfigurationSetupManagerFactory(new String[] { "-f", tcConfig.getAbsolutePath(), "-n",
          "server1" }, StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                new FatalIllegalConfigurationChangeHandler(), null);
      haConfig = new HaConfigImpl(factory.createL2TVSConfigurationSetupManager(null));
      Assert.assertTrue(haConfig.getNodesStore().getAllNodes().length == 1);

      // expecting an error when given non existing server for haConfig
      factory = new StandardConfigurationSetupManagerFactory(new String[] { "-f", tcConfig.getAbsolutePath(), "-n",
          "server2" }, StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                new FatalIllegalConfigurationChangeHandler(), null);
      try {
        new HaConfigImpl(factory.createL2TVSConfigurationSetupManager(null));
        throw new AssertionError("Config setup manager is suppose to blast for non-existing server name");
      } catch (ConfigurationSetupException cse) {
        // expected exception
      }

    } catch (Exception e) {
      throw new AssertionError(e);
    }
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
