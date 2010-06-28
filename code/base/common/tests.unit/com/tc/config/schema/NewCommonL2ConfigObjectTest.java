/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.terracottatech.config.Server;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;

/**
 * Unit/subsystem test for {@link NewCommonL2ConfigObject}.
 */
public class NewCommonL2ConfigObjectTest extends ConfigObjectTestBase {

  private NewCommonL2ConfigObject object;

  public void setUp() throws Exception {
    super.setUp(Server.class);
    this.object = new NewCommonL2ConfigObject(context());
  }

  protected XmlObject getBeanFromTcConfig(TcConfig domainConfig) throws Exception {
    return domainConfig.getServers().getServerArray(0);
  }

  public void xtestConstruction() throws Exception {
    try {
      new NewCommonL2ConfigObject(null);
      fail("Didn't get NPE on no context");
    } catch (NullPointerException npe) {
      // ok
    }
  }

  public void xtestDataPath() throws Exception {
    addListeners(object.dataPath());

    assertEquals(new File("data"), object.dataPath().getFile());
    checkNoListener();

    builder().getServers().getL2s()[0].setData("foobar");
    setConfig();

    assertEquals(new File("foobar"), object.dataPath().getFile());
    checkListener(new File("data"), new File("foobar"));
  }

  public void xtestLogsPath() throws Exception {
    addListeners(object.logsPath());

    assertEquals(new File("logs"), object.logsPath().getFile());
    checkNoListener();

    builder().getServers().getL2s()[0].setLogs("foobar");
    setConfig();

    assertEquals(new File("foobar"), object.logsPath().getFile());
    checkListener(new File("logs"), new File("foobar"));
  }

  public void testJmxPort() throws Exception {
    addListeners(object.jmxPort());

    assertEquals(9520, object.jmxPort().getBindPort());
    checkNoListener();

    builder().getServers().getL2s()[0].setJMXPort("3285");
    setConfig();
    System.out.println(builder().toString());

    assertEquals(3285, object.jmxPort().getBindPort());
    checkListener(new Integer(9520), new Integer(3285));
  }

}
