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

import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.properties.TCPropertiesConsts;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;

public class TcPropertiesWithSpacesOverWriteTest extends TCTestCase {
  private File tcConfig = null;

  public void testOverWrite() throws Exception {
    tcConfig = getTempFile("tc-config-testHaMode1.xml");
    String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
        + "\n<tc-config xmlns=\"http://www.terracotta.org/config\">"
        + "<tc-properties>"
        + "<property name=\"  " + TCPropertiesConsts.L2_HEALTHCHECK_L2_SOCKECT_CONNECT + "  \" value=\"    false    \" />"
        + "<property name=\"  " +   TCPropertiesConsts.LOGGING_MAX_LOGFILE_SIZE  + "  \" value=\"   1234\" />"
        + "</tc-properties>"
        + "\n<servers>"
        + "\n      <server name=\"server1\">"
        + "\n      <tsa-port bind=\"127.8.9.0\">6510</tsa-port>"
        + "\n      <tsa-group-port bind=\"127.8.9.2\">6530</tsa-group-port>"
        + "\n      </server>"
        + "\n</servers>"
        + "\n</tc-config>";
    writeConfigFile(config);
    StandardConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(new String[]{"-f", tcConfig.getAbsolutePath()}, null, null);
    factory.createL2TVSConfigurationSetupManager("server1", getClass().getClassLoader());

    TCProperties tcProps = TCPropertiesImpl.getProperties();
    Assert.assertEquals(false, tcProps.getBoolean(TCPropertiesConsts.L2_HEALTHCHECK_L2_SOCKECT_CONNECT));
    Assert.assertEquals(1234, tcProps.getInt(TCPropertiesConsts.LOGGING_MAX_LOGFILE_SIZE));
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
