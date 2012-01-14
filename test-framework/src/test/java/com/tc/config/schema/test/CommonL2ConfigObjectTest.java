/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.CommonL2ConfigObject;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.terracottatech.config.Server;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;

/**
 * Unit/subsystem test for {@link CommonL2ConfigObject}.
 */
public class CommonL2ConfigObjectTest extends ConfigObjectTestBase {

  private CommonL2ConfigObject object;

  @Override
  public void setUp() throws Exception {
    TcConfig config = TcConfig.Factory.newInstance();
    super.setUp(Server.class);
    L2DSOConfigObject.initializeServers(config, new SchemaDefaultValueProvider(), getTempDirectory());
    setBean(config.getServers().getServerArray(0));
    this.object = new CommonL2ConfigObject(context());
  }

  @Override
  protected XmlObject getBeanFromTcConfig(TcConfig domainConfig) throws Exception {
    return domainConfig.getServers().getServerArray(0);
  }

  public void testConstruction() throws Exception {
    try {
      new CommonL2ConfigObject(null);
      fail("Didn't get NPE on no context");
    } catch (NullPointerException npe) {
      // ok
    }
  }

  public void testDataPath() throws Exception {

    assertEquals(new File(getTempDirectory(), "data"), object.dataPath());
    checkNoListener();

    Server server = (Server) context().bean();
    server.setData("foobar");

    assertEquals(new File("foobar"), object.dataPath());
  }

  public void testLogsPath() throws Exception {

    assertEquals(new File(getTempDirectory(), "logs"), object.logsPath());
    checkNoListener();

    Server server = (Server) context().bean();
    server.setLogs("foobar");

    assertEquals(new File("foobar"), object.logsPath());
  }

  public void testJmxPort() throws Exception {

    assertEquals(9520, object.jmxPort().getIntValue());
    checkNoListener();

    Server server = (Server) context().bean();
    server.getJmxPort().setIntValue(3285);

    assertEquals(3285, object.jmxPort().getIntValue());
  }

}
