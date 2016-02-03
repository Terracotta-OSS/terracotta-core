/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;

public class HaConfigTest extends TCTestCase {
  private File tcConfig = null;

  @SuppressWarnings("unused")
  public void testBasicMakeAllNodes() {
    try {
      tcConfig = getTempFile("tc-config-testFakeL2sName.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc-config xmlns=\"http://www.terracotta.org/config\">" + "\n<servers>"
                      + "\n      <server name=\"server1\" />" + "\n</servers>" + "\n</tc-config>";
      writeConfigFile(config);

      // test for picking up default active server group
      ConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(
                                                                                                    new String[] {
                                                                                                        "-f",
                                                                                                        tcConfig
                                                                                                            .getAbsolutePath() },
                                                                                                    StandardConfigurationSetupManagerFactory.ConfigMode.L2, null);
      HaConfig haConfig = new HaConfigImpl(factory.createL2TVSConfigurationSetupManager(null, getClass().getClassLoader()));
      Assert.assertTrue(haConfig.getNodesStore().getAllNodes().length == 1);

      // test for picking up right active server group for a give server
      factory = new StandardConfigurationSetupManagerFactory(new String[] { "-f", tcConfig.getAbsolutePath(), "-n",
          "server1" }, StandardConfigurationSetupManagerFactory.ConfigMode.L2, null);
      haConfig = new HaConfigImpl(factory.createL2TVSConfigurationSetupManager(null, getClass().getClassLoader()));
      Assert.assertTrue(haConfig.getNodesStore().getAllNodes().length == 1);

      // expecting an error when given non existing server for haConfig
      factory = new StandardConfigurationSetupManagerFactory(new String[] { "-f", tcConfig.getAbsolutePath(), "-n",
          "server2" }, StandardConfigurationSetupManagerFactory.ConfigMode.L2, null);
      try {
        new HaConfigImpl(factory.createL2TVSConfigurationSetupManager(null, getClass().getClassLoader()));
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
