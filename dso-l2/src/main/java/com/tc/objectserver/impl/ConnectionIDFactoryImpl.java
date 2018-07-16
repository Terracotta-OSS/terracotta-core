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
package com.tc.objectserver.impl;

import com.tc.net.StripeID;
import com.tc.util.ProductID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionIDFactoryListener;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.util.Assert;
import com.tc.util.sequence.MutableSequence;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionIDFactoryImpl implements ConnectionIDFactory, DSOChannelManagerEventListener {

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
    // internal or redirect
    if (connectionID.getProductId().isInternal() || this.stripe == null) {
      return internalClients.populateConnectionID(connectionID);
    } else if (!new ChannelID(connectionID.getChannelID()).isValid()) {
      return nextConnectionId(connectionID.getJvmID(), connectionID.getProductId());
    } else if (!stripe.getName().equals(connectionID.getServerID())) {
      return ConnectionID.NULL_ID;
    } else {
      return makeConnectionId(connectionID.getJvmID(), connectionID.getChannelID(), connectionID.getProductId());
    }
  }

  private ConnectionID nextConnectionId(String clientJvmID, ProductID productId) {
    return formConnectionId(clientJvmID, connectionIDSequence.next(), productId);
    }

  private ConnectionID formConnectionId(String jvmID, long channelID, ProductID productId) {
    Assert.assertNotNull(stripe);
    productId = checkCompatibility(productId);
    if (productId == null) {
      return ConnectionID.NULL_ID;
    }
    ConnectionID rv = new ConnectionID(jvmID, channelID, stripe.getName(), null, null, productId);
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
  public void activate(StripeID stripeID, long nextAvailChannelID) {
    this.stripe = stripeID;
    if (nextAvailChannelID >= 0) {
      this.connectionIDSequence.setNext(nextAvailChannelID);
    }
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
  public void channelRemoved(MessageChannel channel, boolean wasActive)  {
    Assert.assertNotNull(stripe);
    ChannelID channelID = channel.getChannelID();
    fireDestroyedEvent(new ConnectionID(ConnectionID.NULL_JVM_ID, channelID.toLong(), stripe.getName(),null , null, channel.getProductID()));
  }

  @Override
  public long getCurrentConnectionID() {
    return connectionIDSequence.current();
  }
}