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
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.transport;

import com.tc.net.ClientID;
import com.tc.net.StripeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.util.ProductID;
import com.tc.util.UUID;


public class DefaultConnectionIdFactory implements ConnectionIDFactory {

  private long         sequence;

  private final String serverID = UUID.getUUID().toString();

  @Override
  public ConnectionID populateConnectionID(ConnectionID connectionID) {
    if (new ChannelID(connectionID.getChannelID()).isNull()) {
      return nextConnectionId(connectionID.getJvmID(), connectionID.getProductId());
    } else {
      return makeConnectionId(connectionID.getJvmID(), connectionID.getChannelID(), connectionID.getProductId());
    }
  }

  private synchronized ConnectionID nextConnectionId(String clientJvmID, ProductID product) {
    return new ConnectionID(clientJvmID, sequence++, serverID, null, null, product);
  }

  private ConnectionID makeConnectionId(String clientJvmID, long channelID, ProductID product) {
    return new ConnectionID(clientJvmID, channelID, serverID, null, null, product);
  }

  @Override
  public void activate(StripeID stripeID, long nextAvailChannelID) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerForConnectionIDEvents(ConnectionIDFactoryListener listener) {
    throw new UnsupportedOperationException();
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

  @Override
  public ConnectionID buildConnectionID(ClientID client) {
    return new ConnectionID(ConnectionID.NULL_JVM_ID, client.getChannelID().toLong(), serverID);
  }
  
  
}
