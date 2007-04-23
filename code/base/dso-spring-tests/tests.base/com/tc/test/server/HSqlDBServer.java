/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server;

import org.hsqldb.HsqlProperties;
import org.hsqldb.Server;
import org.hsqldb.ServerConfiguration;

import com.tc.test.server.appserver.deployment.AbstractDBServer;


public class HSqlDBServer extends AbstractDBServer {
  private static final String DEFAULT_DB_NAME = "testdb";
  private static final int DEFAULT_PORT = ServerConfiguration.getDefaultPort(1, false);
  
  Server server = null;
  
  
  public HSqlDBServer(String name, int port) {
    super();
    
    this.setDbName(name==null ? DEFAULT_DB_NAME : name);
    this.setServerPort(port==0 ? DEFAULT_PORT : port);
  }

  public void doStart() throws Exception {
    HsqlProperties hsqlproperties1 = new HsqlProperties();
    HsqlProperties hsqlproperties2 = HsqlProperties.argArrayToProps(new String[]{
           "-database.0", "mem:test", 
           "-dbname.0", this.getDbName(),
           "server.port", "" + this.getServerPort()
           }, "server");
    hsqlproperties1.addProperties(hsqlproperties2);
    ServerConfiguration.translateDefaultDatabaseProperty(hsqlproperties1);
    server = new Server();
    server.setProperties(hsqlproperties1);
    server.start();    
  }

  public void doStop() throws Exception {
    server.setNoSystemExit(true);
    server.stop();
  }
  
  public String toString() {
    return super.toString() + " dbName:" + this.getDbName() + "; serverPort:" + this.getServerPort();
  }  
}
