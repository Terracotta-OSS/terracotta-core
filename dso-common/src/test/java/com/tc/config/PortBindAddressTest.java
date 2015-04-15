/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.config;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;

public class PortBindAddressTest extends TCTestCase {
  private File tcConfig = null;

  public void testBindPort() {
    try {
      tcConfig = getTempFile("tc-config-testHaMode1.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">"
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">"
                      + "\n      <tsa-port bind=\"127.8.9.0\">6510</tsa-port>"
                      + "\n      <jmx-port bind=\"127.8.9.1\">6520</jmx-port>"
                      + "\n      <tsa-group-port bind=\"127.8.9.2\">6530</tsa-group-port>"
                      + "\n      <management-port bind=\"127.8.9.3\">6540</management-port>"
                      + "\n      </server>"
                      + "\n      <server name=\"server2\">"
                      + "\n      <tsa-port>8510</tsa-port>"
                      + "\n      <jmx-port>8520</jmx-port>"
                      + "\n      <tsa-group-port>8530</tsa-group-port>"
                      + "\n      <management-port>8540</management-port>"
                      + "\n      </server>"
                      + "\n      <server name=\"server3\" bind=\"1.2.3.4\">"
                      + "\n      <tsa-port bind=\"127.8.9.0\">7510</tsa-port>"
                      + "\n      <jmx-port bind=\"127.8.9.1\">7520</jmx-port>"
                      + "\n      <tsa-group-port bind=\"127.8.9.2\">7530</tsa-group-port>"
                      + "\n      <management-port bind=\"127.8.9.3\">7540</management-port>"
                      + "\n      </server>"
                      + "\n      <server name=\"server4\" bind=\"1.2.3.4\">"
                      + "\n      </server>"
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestConfigurationSetupManagerFactory factory = new TestConfigurationSetupManagerFactory(
                                                                                                    TestConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      L2ConfigurationSetupManager configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      Assert.assertEquals("127.8.9.0", configSetupMgr.dsoL2Config().tsaPort().getBind());
      Assert.assertEquals("127.8.9.1", configSetupMgr.commonl2Config().jmxPort().getBind());
      Assert.assertEquals("127.8.9.2", configSetupMgr.dsoL2Config().tsaGroupPort().getBind());
      Assert.assertEquals("127.8.9.3", configSetupMgr.dsoL2Config().managementPort().getBind());
      Assert.assertEquals(6510, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
      Assert.assertEquals(6530, configSetupMgr.dsoL2Config().tsaGroupPort().getIntValue());
      Assert.assertEquals(6520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert.assertEquals(6540, configSetupMgr.commonl2Config().managementPort().getIntValue());

      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
      Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().tsaPort().getBind());
      Assert.assertEquals("0.0.0.0", configSetupMgr.commonl2Config().jmxPort().getBind());
      Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().tsaGroupPort().getBind());
      Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().managementPort().getBind());
      Assert.assertEquals(8510, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
      Assert.assertEquals(8530, configSetupMgr.dsoL2Config().tsaGroupPort().getIntValue());
      Assert.assertEquals(8520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert.assertEquals(8540, configSetupMgr.commonl2Config().managementPort().getIntValue());

      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server3");
      Assert.assertEquals("127.8.9.0", configSetupMgr.dsoL2Config().tsaPort().getBind());
      Assert.assertEquals("127.8.9.1", configSetupMgr.commonl2Config().jmxPort().getBind());
      Assert.assertEquals("127.8.9.2", configSetupMgr.dsoL2Config().tsaGroupPort().getBind());
      Assert.assertEquals("127.8.9.3", configSetupMgr.dsoL2Config().managementPort().getBind());
      Assert.assertEquals(7510, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
      Assert.assertEquals(7530, configSetupMgr.dsoL2Config().tsaGroupPort().getIntValue());
      Assert.assertEquals(7520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert.assertEquals(7540, configSetupMgr.commonl2Config().managementPort().getIntValue());

      configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server4");
      Assert.assertEquals("1.2.3.4", configSetupMgr.dsoL2Config().tsaPort().getBind());
      Assert.assertEquals("1.2.3.4", configSetupMgr.commonl2Config().jmxPort().getBind());
      Assert.assertEquals("1.2.3.4", configSetupMgr.dsoL2Config().tsaGroupPort().getBind());
      Assert.assertEquals("1.2.3.4", configSetupMgr.dsoL2Config().managementPort().getBind());
      Assert.assertEquals(9510, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
      Assert.assertEquals(9530, configSetupMgr.dsoL2Config().tsaGroupPort().getIntValue());
      Assert.assertEquals(9520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert.assertEquals(9540, configSetupMgr.commonl2Config().managementPort().getIntValue());
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
