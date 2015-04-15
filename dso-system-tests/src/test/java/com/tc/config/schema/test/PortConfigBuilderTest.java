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
package com.tc.config.schema.test;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.L2SConfigBuilder;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PortConfigBuilderTest extends TCTestCase {

  private File tcConfig = null;

  public void testPortConfig() throws ConfigurationSetupException, IOException {
    tcConfig = getTempFile("tc-config-testHaMode1.xml");
    writeConfigFile(createConfig().toString());
    TestConfigurationSetupManagerFactory factory = new TestConfigurationSetupManagerFactory(
                                                                                                  TestConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                  null,
                                                                                                  new FatalIllegalConfigurationChangeHandler());
    L2ConfigurationSetupManager configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
    Assert.assertEquals("10.20.30.40", configSetupMgr.dsoL2Config().tsaPort().getBind());
    Assert.assertEquals("1.2.3.4", configSetupMgr.commonl2Config().jmxPort().getBind());
    Assert.assertEquals("4.3.2.1", configSetupMgr.dsoL2Config().tsaGroupPort().getBind());
    Assert.assertEquals("40.30.20.10", configSetupMgr.dsoL2Config().managementPort().getBind());
    Assert.assertEquals(1234, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
    Assert.assertEquals(4321, configSetupMgr.dsoL2Config().tsaGroupPort().getIntValue());
    Assert.assertEquals(1000, configSetupMgr.commonl2Config().jmxPort().getIntValue());
    Assert.assertEquals(1001, configSetupMgr.commonl2Config().managementPort().getIntValue());

    configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server2");
    Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().tsaPort().getBind());
    Assert.assertEquals("0.0.0.0", configSetupMgr.commonl2Config().jmxPort().getBind());
    Assert.assertEquals("5.6.7.8", configSetupMgr.dsoL2Config().tsaGroupPort().getBind());
    Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().managementPort().getBind());
    Assert.assertEquals(6758, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
    Assert.assertEquals(8765, configSetupMgr.dsoL2Config().tsaGroupPort().getIntValue());
    Assert.assertEquals(5678, configSetupMgr.commonl2Config().jmxPort().getIntValue());
    Assert.assertEquals(7658, configSetupMgr.commonl2Config().managementPort().getIntValue());

    configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, "server3");
    Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().tsaPort().getBind());
    Assert.assertEquals("0.0.0.0", configSetupMgr.commonl2Config().jmxPort().getBind());
    Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().tsaGroupPort().getBind());
    Assert.assertEquals("0.0.0.0", configSetupMgr.dsoL2Config().managementPort().getBind());
    Assert.assertEquals(9510, configSetupMgr.dsoL2Config().tsaPort().getIntValue());
    Assert.assertEquals(9530, configSetupMgr.dsoL2Config().tsaGroupPort().getIntValue());
    Assert.assertEquals(9520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
    Assert.assertEquals(9540, configSetupMgr.commonl2Config().managementPort().getIntValue());
  }

  private TerracottaConfigBuilder createConfig() {
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();
    L2SConfigBuilder l2sBuilder = new L2SConfigBuilder();

    L2ConfigBuilder l2Builder1 = new L2ConfigBuilder();
    l2Builder1.setName("server1");
    l2Builder1.setJMXPort(1000);
    l2Builder1.setJMXBindAddress("1.2.3.4");
    l2Builder1.setTSAPort(1234);
    l2Builder1.setTSABindAddress("10.20.30.40");
    l2Builder1.setTSAGroupPort(4321);
    l2Builder1.setTSAGroupPortBindAddress("4.3.2.1");
    l2Builder1.setManagementPort(1001);
    l2Builder1.setManagementPortBindAddress("40.30.20.10");

    L2ConfigBuilder l2Builder2 = new L2ConfigBuilder();
    l2Builder2.setName("server2");
    l2Builder2.setJMXPort(5678);
    l2Builder2.setTSAPort(6758);
    l2Builder2.setTSAGroupPort(8765);
    l2Builder2.setManagementPort(7658);
    l2Builder2.setTSAGroupPortBindAddress("5.6.7.8");

    L2ConfigBuilder l2Builder3 = new L2ConfigBuilder();
    l2Builder3.setName("server3");

    L2ConfigBuilder[] l2Builders = new L2ConfigBuilder[] { l2Builder1, l2Builder2, l2Builder3 };
    l2sBuilder.setL2s(l2Builders);
    out.setServers(l2sBuilder);

    return out;
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
