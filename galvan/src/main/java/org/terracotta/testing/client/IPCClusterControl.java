/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.client;

import java.net.URI;
import java.util.Properties;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.passthrough.IClusterControl;


public class IPCClusterControl implements IClusterControl {
  private final ClientSideIPCManager ipcManager;
  private final URI connectUri;
  private final Properties connectionProperties;

  public IPCClusterControl(ClientSideIPCManager ipcManager, URI connectUri, Properties connectionProperties) {
    this.ipcManager = ipcManager;
    this.connectUri = connectUri;
    this.connectionProperties = connectionProperties;
  }

  @Override
  public Connection createConnectionToActive() {
    Connection connection = null;
    try {
      connection = ConnectionFactory.connect(this.connectUri, this.connectionProperties);
    } catch (ConnectionException e) {
      // We may want to change this API, in the future, but it is only for tests so it is uncertain if we would do anything other than fail, in this scenario.
      throw new RuntimeException("Unexpected exception when creating connection to cluster", e);
    }
    return connection;
  }

  @Override
  public void restartActive() throws Exception {
    ipcManager.restartActive();
  }

  @Override
  public void tearDown() {
    ipcManager.shutDownStripeAndWaitForTermination();
  }

  @Override
  public void waitForActive() throws Exception {
    ipcManager.waitForActive();
  }

  @Override
  public void waitForPassive() throws Exception {
    ipcManager.waitForPassive();
  }
}
