/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.exception.ImplementMe;
import com.terracottatech.configV2.Server;
import com.terracottatech.configV2.Servers;

public class TestL2S extends TestXmlObject implements Servers {

  private Server[] servers;

  public TestL2S(Server[] servers) {
    this.servers = servers;
  }

  public TestL2S() {
    this(null);
  }

  public void setServers(Server[] servers) {
    this.servers = servers;
  }

  public Server[] getServerArray() {
    return this.servers;
  }

  public Server getServerArray(int arg0) {
    return this.servers[arg0];
  }

  public int sizeOfServerArray() {
    return this.servers.length;
  }

  public void setServerArray(Server[] arg0) {
    throw new ImplementMe();
  }

  public void setServerArray(int arg0, Server arg1) {
    throw new ImplementMe();
  }

  public Server insertNewServer(int arg0) {
    throw new ImplementMe();
  }

  public Server addNewServer() {
    throw new ImplementMe();
  }

  public void removeServer(int arg0) {
    throw new ImplementMe();
  }

}
