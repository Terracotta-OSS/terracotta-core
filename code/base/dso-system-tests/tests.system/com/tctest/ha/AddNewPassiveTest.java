/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.ha;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;

import com.tc.config.schema.test.HaConfigBuilder;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.L2SConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.TransparentTestBase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AddNewPassiveTest extends TransparentTestBase {
  private static int                       port1;
  private static int                       port2;
  private static int                       jmxPort1;
  private static int                       jmxPort2;
  private static int                       l2GrpPort1;
  private static int                       l2GrpPort2;
  private static String                    configFile;
  private static ExtraProcessServerControl serverControl;

  public AddNewPassiveTest() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }

  @Override
  protected Class getApplicationClass() {
    return AddNewPassiveTestApp.class;
  }

  @Override
  public void test() throws Exception {
    serverControl.start();

    while (!AddNewPassiveTestApp.checkIfServer2IsActive(jmxPort1)) {
      System.out.println("Trying to find ACTIVE = " + jmxPort1);
      ThreadUtil.reallySleep(1000);
    }

    startANewClient();
  }

  private void startANewClient() throws Exception {
    List jvmArgs = new ArrayList();
    File workDir = new File(getTempDirectory(), "my-client");
    workDir.mkdirs();
    String args[] = AddNewPassiveTestApp.createArgs(port1, port2, jmxPort1, jmxPort2, l2GrpPort1, l2GrpPort2,
                                                    configFile, javaHome.getAbsolutePath());
    ExtraL1ProcessControl client = new ExtraL1ProcessControl("localhost", port1, AddNewPassiveTestApp.class,
                                                             configFile, args, workDir, jvmArgs);
    client.start();
    client.mergeSTDERR();
    client.mergeSTDOUT();
    int exitStatus = client.waitFor();
    Assert.assertEquals(0, exitStatus);
  }

  public void setUp() throws Exception {
    setJavaHome();

    PortChooser pc = new PortChooser();
    port1 = pc.chooseRandomPort();
    jmxPort1 = pc.chooseRandomPort();
    l2GrpPort1 = pc.chooseRandomPort();

    port2 = pc.chooseRandomPort();
    jmxPort2 = pc.chooseRandomPort();
    l2GrpPort2 = pc.chooseRandomPort();

    configFile = getTempFile("tc-config.xml").getAbsolutePath();

    writeConfigFile(true, configFile, port1, port2, jmxPort1, jmxPort2, l2GrpPort1, l2GrpPort2);

    serverControl = new ExtraProcessServerControl("localhost", port1, jmxPort1, configFile, true, javaHome,
                                                  new ArrayList());
  }

  public static void writeConfigFile(boolean only1Server, String cfgFile, int dsoPort1, int dsoPort2, int jPort1,
                                     int jPort2, int l2Port1, int l2Port2) {
    try {
      TerracottaConfigBuilder builder = createConfig(only1Server, dsoPort1, dsoPort2, jPort1, jPort2, l2Port1, l2Port2);
      FileOutputStream out = new FileOutputStream(new File(cfgFile));
      IOUtils.copy(new StringInputStream(builder.toString()), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public static TerracottaConfigBuilder createConfig(boolean only1Server, int dsoPort1, int dsoPort2, int jPort1,
                                                     int jPort2, int l2Port1, int l2Port2) {
    TerracottaConfigBuilder builder = AddNewPassiveTestApp.getTerracottaConfigBuilder();
    int length = 1;
    if (!only1Server) {
      length = 2;
    }
    L2ConfigBuilder[] l2Builder = new L2ConfigBuilder[length];

    int[] dsoPorts = { dsoPort1, dsoPort2 };
    int[] jmxPorts = { jPort1, jPort2 };
    int[] l2GrpPorts = { l2Port1, l2Port2 };

    HaConfigBuilder haBuilder = new HaConfigBuilder();
    haBuilder.setMode(HaConfigBuilder.HA_MODE_NETWORKED_ACTIVE_PASSIVE);

    for (int i = 0; i < l2Builder.length; i++) {
      l2Builder[i] = new L2ConfigBuilder();
      l2Builder[i].setJMXPort(jmxPorts[i]);
      l2Builder[i].setDSOPort(dsoPorts[i]);
      l2Builder[i].setL2GroupPort(l2GrpPorts[i]);
      l2Builder[i].setName(i + "-localhost");
      l2Builder[i].setData(i + "-data");
      l2Builder[i].setLogs(i + "-logs");
      l2Builder[i].setStatistics(i + "-stats");
    }
    L2SConfigBuilder l2SBuilder = new L2SConfigBuilder();
    l2SBuilder.setL2s(l2Builder);
    l2SBuilder.setHa(haBuilder);
    builder.setServers(l2SBuilder);

    return builder;
  }

  @Override
  protected void tearDown() throws Exception {
    // No op - the server is shutdown by the client
  }

}
