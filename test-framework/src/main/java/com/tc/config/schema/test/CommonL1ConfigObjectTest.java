/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.CommonL1ConfigObject;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.object.config.schema.L1DSOConfigObject;
import com.terracottatech.config.Client;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.net.InetAddress;

/**
 * Unit/subsystem test for {@link CommonL1ConfigObject}.
 */
public class CommonL1ConfigObjectTest extends ConfigObjectTestBase {

  private CommonL1ConfigObject object;

  @Override
  public void setUp() throws Exception {
    TcConfig config = TcConfig.Factory.newInstance();
    super.setUp(Client.class);
    L1DSOConfigObject.initializeClients(config, new SchemaDefaultValueProvider());
    setBean(config.getClients());
    this.object = new CommonL1ConfigObject(context());
  }

  @Override
  protected XmlObject getBeanFromTcConfig(TcConfig config) throws Exception {
    return config.getClients();
  }

  public void testConstruction() throws Exception {
    try {
      new CommonL1ConfigObject(null);
      fail("Didn't get NPE on no context");
    } catch (NullPointerException npe) {
      // ok
    }
  }

  public void testLogsPath() throws Exception {

    assertEquals(new File(getTempDirectory().getParent(), "logs-" + InetAddress.getLocalHost().getHostAddress()),
                 object.logsPath());
    checkNoListener();

    Client client = (Client) context().bean();
    client.setLogs("foobar");

    assertEquals(new File("foobar"), object.logsPath());
  }

}
