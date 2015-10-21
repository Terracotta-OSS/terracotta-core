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

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;

public class PortBindAddressTest extends TCTestCase {
  private File tcConfig = null;

  public void testBindPort() {
    try {
      tcConfig = getTempFile("tc-config-testHaMode1.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc-config xmlns=\"http://www.terracotta.org/config\">"
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">"
                      + "\n      <tsa-port bind=\"127.8.9.0\">6510</tsa-port>"
                      + "\n      <tsa-group-port bind=\"127.8.9.2\">6530</tsa-group-port>"
                      + "\n      </server>"
                      + "\n      <server name=\"server2\">"
                      + "\n      <tsa-port>8510</tsa-port>"
                      + "\n      <tsa-group-port>8530</tsa-group-port>"
                      + "\n      </server>"
                      + "\n      <server name=\"server3\" bind=\"1.2.3.4\">"
                      + "\n      <tsa-port bind=\"127.8.9.0\">7510</tsa-port>"
                      + "\n      <tsa-group-port bind=\"127.8.9.2\">7530</tsa-group-port>"
                      + "\n      </server>"
                      + "\n      <server name=\"server4\" bind=\"1.2.3.4\">"
                      + "\n      </server>"
                      + "\n</servers>"
                      + "\n</tc-config>";
      writeConfigFile(config);
      StandardConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(new String[]{"-f", tcConfig.getAbsolutePath()}, null, null);
      L2ConfigurationSetupManager configSetupMgr = factory.createL2TVSConfigurationSetupManager("server1");
      Assert.assertEquals("127.8.9.0", configSetupMgr.dsoL2Config().tsaPort().getBind());
      Assert.assertEquals("127.8.9.2", configSetupMgr.dsoL2Config().tsaGroupPort().getBind());
      Assert.assertEquals(6510, configSetupMgr.dsoL2Config().tsaPort().getValue());
      Assert.assertEquals(6530, configSetupMgr.dsoL2Config().tsaGroupPort().getValue());

      configSetupMgr = factory.createL2TVSConfigurationSetupManager("server2");
      Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().tsaPort().getBind());
      Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().tsaGroupPort().getBind());
      Assert.assertEquals(8510, configSetupMgr.dsoL2Config().tsaPort().getValue());
      Assert.assertEquals(8530, configSetupMgr.dsoL2Config().tsaGroupPort().getValue());

      configSetupMgr = factory.createL2TVSConfigurationSetupManager("server3");
      Assert.assertEquals("127.8.9.0", configSetupMgr.dsoL2Config().tsaPort().getBind());
      Assert.assertEquals("127.8.9.2", configSetupMgr.dsoL2Config().tsaGroupPort().getBind());
      Assert.assertEquals(7510, configSetupMgr.dsoL2Config().tsaPort().getValue());
      Assert.assertEquals(7530, configSetupMgr.dsoL2Config().tsaGroupPort().getValue());

      configSetupMgr = factory.createL2TVSConfigurationSetupManager("server4");
      Assert.assertEquals("1.2.3.4", configSetupMgr.dsoL2Config().tsaPort().getBind());
      Assert.assertEquals("1.2.3.4", configSetupMgr.dsoL2Config().tsaGroupPort().getBind());
      Assert.assertEquals(9510, configSetupMgr.dsoL2Config().tsaPort().getValue());
      Assert.assertEquals(9530, configSetupMgr.dsoL2Config().tsaGroupPort().getValue());
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
