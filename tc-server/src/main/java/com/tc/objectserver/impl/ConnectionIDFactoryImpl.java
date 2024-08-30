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
package com.tc.objectserver.impl;

import com.tc.net.StripeID;
import com.tc.net.core.ProductID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionIDFactoryListener;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.util.Assert;
import com.tc.util.sequence.MutableSequence;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionIDFactoryImpl implements ConnectionIDFactory, ChannelManagerEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionIDFactoryImpl.class);
  private final MutableSequence                   connectionIDSequence;
  private final ConnectionIDFactory                        internalClients;
  private StripeID                                stripe;
  private final Set<ProductID>                          supported;
  private final List<ConnectionIDFactoryListener> listeners = new CopyOnWriteArrayList<>();

  public ConnectionIDFactoryImpl(ConnectionIDFactory internals, ClientStatePersistor clientStateStore, Set<ProductID> supported) {
    this.internalClients = internals;
    this.connectionIDSequence = clientStateStore.getConnectionIDSequence();
    this.supported = supported;
  }

  @Override
  public ConnectionID populateConnectionID(ConnectionID connectionID) {
    StripeID stripeid = getStripeID();
    if (stripeid == null || !connectionID.getProductId().isReconnectEnabled()) {
    // internal client on active or diagnostic
      ConnectionID cid = internalClients.populateConnectionID(connectionID);
      return cid;
    } else if (!new ChannelID(connectionID.getChannelID()).isValid()) {
      return nextConnectionId(connectionID.getJvmID(), connectionID.getProductId());
    } else if (!stripeid.getName().equals(connectionID.getServerID())) {
      return ConnectionID.NULL_ID;
    } else {
      return makeConnectionId(connectionID.getJvmID(), connectionID.getChannelID(), connectionID.getProductId());
    }
  }

  private ConnectionID nextConnectionId(String clientJvmID, ProductID productId) {
    return formConnectionId(clientJvmID, connectionIDSequence.next(), productId);
    }

  private ConnectionID formConnectionId(String jvmID, long channelID, ProductID productId) {
    Assert.assertNotNull(getStripeID());
    productId = checkCompatibility(productId);
    if (productId == null) {
      return ConnectionID.NULL_ID;
    }
    ConnectionID rv = new ConnectionID(jvmID, channelID, getStripeID().getName(), productId);
    fireCreationEvent(rv);
    return rv;
  }
  
  private ProductID checkCompatibility(ProductID productId) {
    // this is confusing but StripeID is being used as serverID.  When a passive transitions to 
    // active and allows reconnects, it expects the serverID to match which can only be the stripeID
    if (!supported.contains(productId)) {
      switch (productId) {
        case PERMANENT:
          if (supported.contains(ProductID.STRIPE)) {
            productId = ProductID.STRIPE;
            break;
          }
          //  fall through to next level if not supported
        case STRIPE:
          if (supported.contains(ProductID.SERVER)) {
            productId = ProductID.SERVER;
            break;
          }
          //  fall through to next level if not supported
        case SERVER:
          //  nothing supported, return the null id
          productId = null;
      }
    }
    return productId;
  }

  private ConnectionID makeConnectionId(String clientJvmID, long channelID, ProductID productId) {
    Assert.assertTrue(channelID != ChannelID.NULL_ID.toLong());
    return formConnectionId(clientJvmID, channelID, productId);
  }

  private void fireCreationEvent(ConnectionID rv) {
    for (ConnectionIDFactoryListener listener : listeners) {
      listener.connectionIDCreated(rv);
    }
  }

  private void fireDestroyedEvent(ConnectionID connectionID) {
    for (ConnectionIDFactoryListener listener : listeners) {
      listener.connectionIDDestroyed(connectionID);
    }
  }

  @Override
  public synchronized void activate(StripeID stripeID, long nextAvailChannelID) {
    this.stripe = stripeID;
    LOGGER.info("activating connectionid factory stripe:{} next id: {}", stripeID, nextAvailChannelID);
    if (nextAvailChannelID >= 0) {
      this.connectionIDSequence.setNext(nextAvailChannelID);
    }
  }

  private synchronized StripeID getStripeID() {
    return this.stripe;
  }

  @Override
  public void registerForConnectionIDEvents(ConnectionIDFactoryListener listener) {
    listeners.add(listener);
  }

  @Override
  public void channelCreated(MessageChannel channel) {
    // NOP
  }

  @Override
  public void channelRemoved(MessageChannel channel)  {
    Assert.assertNotNull(getStripeID());
    if (channel.getProductID().isReconnectEnabled()) {
      fireDestroyedEvent(channel.getConnectionID());
    }
  }

  @Override
  public long getCurrentConnectionID() {
    return connectionIDSequence.current();
  }
}