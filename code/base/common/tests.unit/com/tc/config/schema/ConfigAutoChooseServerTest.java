/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;

public class ConfigAutoChooseServerTest extends TCTestCase {
  private File tcConfig = null;

  public void testConfigAutoChooseThisL2() {
    try {
      tcConfig = getTempFile("tc-config-test.xml");
      String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "\n<servers>"
                      + "\n      <server name=\"server1\" host=\"%i\">" + "\n       <data>"
                      + System.getProperty("user.home")
                      + "/terracotta/server1-data</data>"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server1-logs</logs>"
                      + "\n       <statistics>"
                      + System.getProperty("user.home")
                      + "/terracotta/server1-stats</statistics>"
                      + "\n       <dso-port>9510</dso-port>"
                      + "\n       <jmx-port>9520</jmx-port>"
                      + "\n       <l2-group-port>9530</l2-group-port>"
                      + "\n      <dso>"
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>"
                      + "\n      </dso>"
                      + "\n      </server>"
                      + "\n      <server name=\"server2\" host=\"11.0.1.2\">"
                      + "\n       <data>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-data</data>"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-logs</logs>"
                      + "\n       <statistics>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-stats</statistics>"
                      + "\n       <dso-port>8510</dso-port>"
                      + "\n      <dso>"
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>"
                      + "\n      </dso>"
                      + "\n</server>"
                      + "\n      <server name=\"server3\" host=\"11.0.1.3\">"
                      + "\n       <data>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-data</data>"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-logs</logs>"
                      + "\n       <statistics>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-stats</statistics>"
                      + "\n       <dso-port>7510</dso-port>"
                      + "\n       <l2-group-port>7555</l2-group-port>"
                      + "\n      <dso>"
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>"
                      + "\n        </persistence>"
                      + "\n      </dso>"
                      + "\n</server>"
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestConfigurationSetupManagerFactory factory = new TestConfigurationSetupManagerFactory(
                                                                                                    new FatalIllegalConfigurationChangeHandler());

      L2ConfigurationSetupManager configSetupMgr = factory.createL2TVSConfigurationSetupManager(tcConfig, null);
      Assert.assertEquals(9510, configSetupMgr.dsoL2Config().dsoPort().getIntValue());
      Assert.assertEquals(9520, configSetupMgr.commonl2Config().jmxPort().getIntValue());
      Assert.assertEquals(9530, configSetupMgr.dsoL2Config().l2GroupPort().getIntValue());

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

  @Override
  protected boolean cleanTempDir() {
    return false;
  }

}
