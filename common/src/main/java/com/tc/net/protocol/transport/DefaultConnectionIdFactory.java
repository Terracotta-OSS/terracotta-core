/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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

import com.tc.net.StripeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.core.ProductID;
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
    return new ConnectionID(clientJvmID, sequence++, serverID, product);
  }

  private ConnectionID makeConnectionId(String clientJvmID, long channelID, ProductID product) {
    return new ConnectionID(clientJvmID, channelID, serverID, product);
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
  public long getCurrentConnectionID() {
    return sequence;
  }

  public String getServerID() {
    return serverID;
  }
}
