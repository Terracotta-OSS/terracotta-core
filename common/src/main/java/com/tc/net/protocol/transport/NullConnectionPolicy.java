/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.transport;

public class NullConnectionPolicy implements ConnectionPolicy {

  @Override
  public boolean connectClient(ConnectionID id) {
    return true;
  }

  @Override
  public boolean clientDisconnected(ConnectionID id) {
    return false;
  }

  @Override
  public boolean isMaxConnectionsReached() {
    return false;
  }

  @Override
  public int getMaxConnections() {
    return -1;
  }

  @Override
  public int getNumberOfActiveConnections() {
    return 0;
  }

  @Override
  public int getConnectionHighWatermark() {
    return 0;
  }

  @Override
  public boolean isConnectAllowed(ConnectionID id) {
    return true;
  }

}
