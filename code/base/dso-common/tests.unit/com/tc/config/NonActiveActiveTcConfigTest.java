/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;

public class NonActiveActiveTcConfigTest extends TCTestCase {
  private File tcConfig = null;
  
  public NonActiveActiveTcConfigTest() {
    disableAllUntil("2009-02-20");
  }
  
  public void testServerInTwoGroups() {
    try {
      tcConfig = getTempFile("tc-config-testServerInTwoGroups.xml");
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
                      + "\n      <server name=\"server2\">" 
                      + "\n      <dso>" 
                      + "\n        <persistence>"
                      + "\n          <mode>permanent-store</mode>" 
                      + "\n        </persistence>" 
                      + "\n      </dso>"
                      + "\n      </server>" 
                      + "\n      <active-server-groups>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server1</member>" 
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n          <active-server-group>"
                      + "\n              <members>" 
                      + "\n                <member>server2</member>" 
                      + "\n              </members>"
                      + "\n              <ha>" 
                      + "\n                <mode>networked-active-passive</mode>"
                      + "\n                <networked-active-passive>"
                      + "\n                  <election-time>1</election-time>"
                      + "\n                </networked-active-passive>" 
                      + "\n              </ha>"
                      + "\n          </active-server-group>" 
                      + "\n      </active-server-groups>" 
                      + "\n</servers>"
                      + "\n</tc:tc-config>";
      writeConfigFile(config);
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      throw new AssertionError("Multiple groups cannot start with Open source version");
    } catch (ConfigurationSetupException e) {
      // expected exception
      System.out.println("Expected Exception.");
      System.out.println(e.getMessage());
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
