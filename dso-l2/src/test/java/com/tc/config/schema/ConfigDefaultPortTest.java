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
package com.tc.config.schema;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.object.config.schema.L2ConfigObject;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import org.apache.commons.io.IOUtils;
import org.terracotta.config.TCConfigDefaults;

import java.io.File;
import java.io.FileOutputStream;

public class ConfigDefaultPortTest extends TCTestCase {
  private File tcConfig = null;

  public void testConfigDefaultDSOJmxGroupports() {
    try {
      tcConfig = getTempFile("tc-config-test.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "\n<tc-config xmlns=\"http://www.terracotta.org/config\">"
                      + "\n<servers>" + "\n      <server name=\"server1\">"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server1-logs</logs>"
                      + "\n       <tsa-port>19510</tsa-port>"
                      + "\n       <tsa-group-port>19530</tsa-group-port>"
                      + "\n      </server>"
                      + "\n      <server name=\"server2\">"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-logs</logs>"
                      + "\n       <tsa-port>8510</tsa-port>"
                      + "\n</server>"
                      + "\n      <server name=\"server3\">"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-logs</logs>"
                      + "\n       <tsa-port>7510</tsa-port>"
                      + "\n       <tsa-group-port>7555</tsa-group-port>"
                      + "\n</server>"
                      + "\n      <server name=\"server5\">"
                      + "\n       <tsa-port>65534</tsa-port>" + "\n</server>"

                      + "\n      <server name=\"server4\">" + "\n</server>" + "\n</servers>" + "\n</tc-config>";
      writeConfigFile(config);
      StandardConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(new String[]{"-f", tcConfig.getAbsolutePath()}, null, null);

      L2ConfigurationSetupManager configSetupMgr = null;

      // case 1: all ports specified in the config
      configSetupMgr = factory.createL2TVSConfigurationSetupManager("server1", getClass().getClassLoader());
      Assert.assertEquals(19510, configSetupMgr.dsoL2Config().tsaPort().getValue());
      Assert.assertEquals(19530, configSetupMgr.dsoL2Config().tsaGroupPort().getValue());

      // case 2: just tsa-port specified in the config; other port numbers are calculated
      configSetupMgr = factory.createL2TVSConfigurationSetupManager("server2", getClass().getClassLoader());
      Assert.assertEquals(8510, configSetupMgr.dsoL2Config().tsaPort().getValue());
      Assert.assertEquals(8510 + L2ConfigObject.DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT, configSetupMgr.dsoL2Config().tsaGroupPort()
          .getValue());

      // case 3: tsa-port and group-port specified; jmx-port, management-port calculated
      configSetupMgr = factory.createL2TVSConfigurationSetupManager("server3", getClass().getClassLoader());
      Assert.assertEquals(7510, configSetupMgr.dsoL2Config().tsaPort().getValue());
      Assert.assertEquals(7555, configSetupMgr.dsoL2Config().tsaGroupPort().getValue());

      // case 4: all ports are default
      configSetupMgr = factory.createL2TVSConfigurationSetupManager("server4", getClass().getClassLoader());
      Assert.assertEquals(TCConfigDefaults.TSA_PORT, configSetupMgr.dsoL2Config().tsaPort().getValue());
      Assert.assertEquals(TCConfigDefaults.GROUP_PORT, configSetupMgr.dsoL2Config().tsaGroupPort().getValue());

      // case 5: ports range overflow
      configSetupMgr = factory.createL2TVSConfigurationSetupManager("server5", getClass().getClassLoader());
      Assert.assertEquals(65534, configSetupMgr.dsoL2Config().tsaPort().getValue());
      Assert.assertEquals(((65534 + L2ConfigObject.DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT) % L2ConfigObject.MAX_PORTNUMBER)
                          + L2ConfigObject.MIN_PORTNUMBER, configSetupMgr.dsoL2Config().tsaGroupPort().getValue());

    } catch (Throwable e) {
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

  @Override
  protected boolean cleanTempDir() {
    return false;
  }

}
