/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.deployment;


public abstract class AbstractDBServer extends AbstractStoppable implements Stoppable {
    private int serverPort = 0;
    private String dbName = null;

    public int getServerPort() {
      return serverPort;
    }

    public void setServerPort(int serverPort) {
      this.serverPort = serverPort;
    } 

    public String getDbName() {
      return dbName;
    }

    public void setDbName(String dbName) {
      this.dbName = dbName;
    }
}
