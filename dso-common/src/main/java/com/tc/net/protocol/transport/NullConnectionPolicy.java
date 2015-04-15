/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.transport;

public class NullConnectionPolicy implements ConnectionPolicy {

  @Override
  public boolean connectClient(ConnectionID id) {
    return true;
  }

  @Override
  public void clientDisconnected(ConnectionID id) {
    return;
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
