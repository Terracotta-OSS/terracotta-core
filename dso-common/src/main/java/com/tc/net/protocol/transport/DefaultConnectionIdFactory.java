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

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.util.UUID;

import java.util.Collections;
import java.util.Set;

public class DefaultConnectionIdFactory implements ConnectionIDFactory {

  private long         sequence;

  private final String serverID = UUID.getUUID().toString();

  @Override
  public ConnectionID populateConnectionID(final ConnectionID connectionID) {
    if (new ChannelID(connectionID.getChannelID()).isNull()) {
      return nextConnectionId(connectionID.getJvmID());
    } else {
      return makeConnectionId(connectionID.getJvmID(), connectionID.getChannelID());
    }
  }

  private synchronized ConnectionID nextConnectionId(String clientJvmID) {
    return new ConnectionID(clientJvmID, sequence++, serverID);
  }

  private ConnectionID makeConnectionId(String clientJvmID, long channelID) {
    return new ConnectionID(clientJvmID, channelID, serverID);
  }

  @Override
  public Set<ConnectionID> loadConnectionIDs() {
    return Collections.EMPTY_SET;
  }

  @Override
  public void init(String clusterID, long nextAvailChannelID, Set<ConnectionID> connections) {
    throw new ImplementMe();
  }

  @Override
  public void registerForConnectionIDEvents(ConnectionIDFactoryListener listener) {
    throw new ImplementMe();
  }

  @Override
  public void restoreConnectionId(ConnectionID rv) {
    //
  }

  @Override
  public long getCurrentConnectionID() {
    return sequence;
  }

  public String getServerID() {
    return serverID;
  }
}
