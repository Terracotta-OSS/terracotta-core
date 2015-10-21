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

import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
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
                      + "\n<tc-config xmlns=\"http://www.terracotta.org/config\">" + "\n<servers>"
                      + "\n      <server name=\"server1\" host=\"%i\">"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server1-logs</logs>"
                      + "\n       <tsa-port>9510</tsa-port>"
                      + "\n       <tsa-group-port>9530</tsa-group-port>"
                      + "\n      </server>"
                      + "\n      <server name=\"server2\" host=\"11.0.1.2\">"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-logs</logs>"
                      + "\n       <tsa-port>8510</tsa-port>"
                      + "\n</server>"
                      + "\n      <server name=\"server3\" host=\"11.0.1.3\">"
                      + "\n       <logs>"
                      + System.getProperty("user.home")
                      + "/terracotta/server2-logs</logs>"
                      + "\n       <tsa-port>7510</tsa-port>"
                      + "\n       <tsa-group-port>7555</tsa-group-port>"
                      + "\n</server>"
                      + "\n</servers>"
                      + "\n</tc-config>";
      writeConfigFile(config);
      StandardConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(new String[]{"-f", tcConfig.getAbsolutePath()}, null, null);

      L2ConfigurationSetupManager configSetupMgr = factory.createL2TVSConfigurationSetupManager(null);
      Assert.assertEquals(9510, configSetupMgr.dsoL2Config().tsaPort().getValue());
      Assert.assertEquals(9530, configSetupMgr.dsoL2Config().tsaGroupPort().getValue());

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
