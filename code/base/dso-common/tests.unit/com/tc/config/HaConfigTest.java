/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
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
                      + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" 
                      + "\n<servers>"
                      + "\n      <server name=\"server1\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n</servers>" 
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      
      //test for picking up default active server group
      TVSConfigurationSetupManagerFactory factory = new StandardTVSConfigurationSetupManagerFactory(new String[] {
          "-f", tcConfig.getAbsolutePath() }, true, new FatalIllegalConfigurationChangeHandler());
      HaConfig haConfig = new HaConfigImpl(factory.createL2TVSConfigurationSetupManager(null));
      Assert.assertTrue(haConfig.makeThisGroupNodes().length == 1);

      //test for picking up right active server group for a give server
      factory = new StandardTVSConfigurationSetupManagerFactory(new String[] { "-f", tcConfig.getAbsolutePath(), "-n",
          "server1" }, true, new FatalIllegalConfigurationChangeHandler());
      haConfig = new HaConfigImpl(factory.createL2TVSConfigurationSetupManager(null));
      Assert.assertTrue(haConfig.makeThisGroupNodes().length == 1);

      //expecting an error when given non existing server for haConfig
      factory = new StandardTVSConfigurationSetupManagerFactory(new String[] { "-f", tcConfig.getAbsolutePath(), "-n",
          "server2" }, true, new FatalIllegalConfigurationChangeHandler());
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
