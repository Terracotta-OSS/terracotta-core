/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.net.URI;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;


/**
 * The implementation used to control the passthrough testing cluster.
 */
public class PassthroughClusterControl implements IClusterControl {
  private final String stripeName;
  // We track the "original" server state for tear-down.
  private final PassthroughServer originalActiveServer;
  private final PassthroughServer originalPassiveServer;

  // The active we are currently using can change on restart.
  private PassthroughServer activeServer;

  public PassthroughClusterControl(String stripeName, PassthroughServer activeServer, PassthroughServer passiveServer) {
    this.stripeName = stripeName;
    // The active cannot be null but the passive can be.
    Assert.assertTrue(null != activeServer);
    this.originalActiveServer = activeServer;
    this.originalPassiveServer = passiveServer;
    // We set the changing active server to be the original.
    this.activeServer = activeServer;
    PassthroughServer previous = PassthroughServerRegistry.getSharedInstance().registerServer(this.stripeName, activeServer);
    Assert.assertTrue(null == previous);
  }

  @Override
  public void restartActive() throws Exception {
    this.activeServer = this.activeServer.restart();
  }

  @Override
  public void waitForActive() throws Exception {
    // Do nothing - the restart brings up the active, immediately.
  }

  @Override
  public void waitForPassive() throws Exception {
    // Do nothing - the restart brings up the active, immediately.
  }

  @Override
  public Connection createConnectionToActive() {
    URI uri = URI.create("passthrough://" + this.stripeName);
    Connection connection = null;
    try {
      connection = ConnectionFactory.connect(uri, null);
    } catch (ConnectionException e) {
      Assert.unexpected(e);
    }
    return connection;
  }

  @Override
  public void tearDown() {
    this.originalActiveServer.stop();
    if (null != this.originalPassiveServer) {
      this.originalPassiveServer.stop();
    }
    PassthroughServer removedServer = PassthroughServerRegistry.getSharedInstance().unregisterServer(this.stripeName);
    Assert.assertTrue(this.originalActiveServer == removedServer);
  }
}
