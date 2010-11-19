/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.object.config.schema.NewL1DSOConfigObject;
import com.terracottatech.config.Client;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.net.InetAddress;

/**
 * Unit/subsystem test for {@link NewCommonL1ConfigObject}.
 */
public class NewCommonL1ConfigObjectTest extends ConfigObjectTestBase {

  private NewCommonL1ConfigObject object;

  @Override
  public void setUp() throws Exception {
    TcConfig config = TcConfig.Factory.newInstance();
    super.setUp(Client.class);
    NewL1DSOConfigObject.initializeClients(config, new SchemaDefaultValueProvider());
    setBean(config.getClients());
    this.object = new NewCommonL1ConfigObject(context());
  }

  @Override
  protected XmlObject getBeanFromTcConfig(TcConfig config) throws Exception {
    return config.getClients();
  }

  public void testConstruction() throws Exception {
    try {
      new NewCommonL1ConfigObject(null);
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
