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

import org.terracotta.passthrough.IClusterControl;


/**
 * The {@link IClusterControl} implementation which is instantiated on the client-side for the running test.
 */
public class IPCClusterControl implements IClusterControl {
  private final ClientSideIPCManager ipcManager;

  public IPCClusterControl(ClientSideIPCManager ipcManager) {
    this.ipcManager = ipcManager;
  }

  @Override
  public void waitForActive() throws Exception {
    ipcManager.waitForActive();
  }

  @Override
  public void waitForRunningPassivesInStandby() throws Exception {
    ipcManager.waitForPassive();
  }

  @Override
  public void startOneServer() throws Exception {
    ipcManager.startOneServer();
  }

  @Override
  public void startAllServers() throws Exception {
    ipcManager.startAllServers();
  }

  @Override
  public void terminateActive() throws Exception {
    ipcManager.terminateActive();
  }

  @Override
  public void terminateOnePassive() throws Exception {
    ipcManager.terminateOnePassive();
  }

  @Override
  public void terminateAllServers() {
    ipcManager.shutDownStripeAndWaitForTermination();
  }
}
