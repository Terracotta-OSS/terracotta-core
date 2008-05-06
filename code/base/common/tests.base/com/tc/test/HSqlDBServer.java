/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import org.hsqldb.HsqlProperties;
import org.hsqldb.Server;
import org.hsqldb.ServerConfiguration;

// Mainly copy from HsqlDBServer class of dso-spring-tests modules.
public class HSqlDBServer {
  private static final String DEFAULT_DB_NAME = "testdb";
  private static final int    DEFAULT_PORT    = 9001;

  private Server              server;

  public HSqlDBServer() {
    super();
  }

  public void start() throws Exception {
    HsqlProperties hsqlproperties1 = new HsqlProperties();
    HsqlProperties hsqlproperties2 = HsqlProperties.argArrayToProps(new String[] { "-database.0", "mem:testdb",
        "-dbname.0", DEFAULT_DB_NAME, "server.port", "" + DEFAULT_PORT }, "server");
    hsqlproperties1.addProperties(hsqlproperties2);
    ServerConfiguration.translateDefaultDatabaseProperty(hsqlproperties1);
    server = new Server();
    server.setProperties(hsqlproperties1);
    server.start();
  }

  public void stop() throws Exception {
    server.setNoSystemExit(true);
    server.stop();
  }

  public static void main(String[] args) {
    try {
      HSqlDBServer dbServer = new HSqlDBServer();
      dbServer.start();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
