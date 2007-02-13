/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.terracottatech.config.Client;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;

/**
 * Unit/subsystem test for {@link NewCommonL1ConfigObject}.
 */
public class NewCommonL1ConfigObjectTest extends ConfigObjectTestBase {

  private NewCommonL1ConfigObject object;

  public void setUp() throws Exception {
    super.setUp(Client.class);
    this.object = new NewCommonL1ConfigObject(context());
  }

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
    addListeners(object.logsPath());

    String theString = object.logsPath().getFile().toString();
    assertTrue(theString.startsWith("logs-"));
    checkNoListener();

    builder().getClient().setLogs("foobar");
    setConfig();
    assertEquals(new File("foobar"), object.logsPath().getFile());
    checkListener(new File(theString), new File("foobar"));
  }

}
