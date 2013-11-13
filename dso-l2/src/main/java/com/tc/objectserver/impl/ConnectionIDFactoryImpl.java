/*
 * All content copyright (c) 2003-2011 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.license.ProductID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionIDFactoryListener;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.api.ClientNotFoundException;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.util.Assert;
import com.tc.util.sequence.MutableSequence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionIDFactoryImpl implements ConnectionIDFactory, DSOChannelManagerEventListener {

  private final ClientStatePersistor              clientStateStore;
  private final MutableSequence                   connectionIDSequence;
  private String                                  uid;
  private final List<ConnectionIDFactoryListener> listeners = new CopyOnWriteArrayList<ConnectionIDFactoryListener>();

  public ConnectionIDFactoryImpl(ClientStatePersistor clientStateStore) {
    this.clientStateStore = clientStateStore;
    this.connectionIDSequence = clientStateStore.getConnectionIDSequence();
    this.uid = connectionIDSequence.getUID();
  }

  @Override
  public ConnectionID populateConnectionID(final ConnectionID connectionID) {
    if (new ChannelID(connectionID.getChannelID()).isNull()) {
      return nextConnectionId(connectionID.getJvmID(), connectionID.getProductId());
    } else {
      return makeConnectionId(connectionID.getJvmID(), connectionID.getChannelID(), connectionID.getProductId());
    }
  }

  private ConnectionID nextConnectionId(String clientJvmID, final ProductID productId) {
    return buildConnectionId(clientJvmID, connectionIDSequence.next(), productId);
  }

  private ConnectionID buildConnectionId(String jvmID, long channelID, final ProductID productId) {
    Assert.assertNotNull(uid);
    // Make sure we save the fact that we are giving out this id to someone in the database before giving it out.
    clientStateStore.saveClientState(new ChannelID(channelID));
    ConnectionID rv = new ConnectionID(jvmID, channelID, uid, null, null, productId);
    fireCreationEvent(rv);
    return rv;
  }

  private ConnectionID makeConnectionId(String clientJvmID, long channelID, final ProductID productId) {
    Assert.assertTrue(channelID != ChannelID.NULL_ID.toLong());
    // provided channelID shall not be using
    if (clientStateStore.containsClient(new ChannelID(channelID))) { throw new TCRuntimeException(
                                                                                                  "The connectionId "
                                                                                                      + channelID
                                                                                                      + " has been used. "
                                                                                                      + " One possible cause: restarted some mirror groups but not all."); }

    return buildConnectionId(clientJvmID, channelID, productId);
  }

  @Override
  public void restoreConnectionId(ConnectionID rv) {
    fireCreationEvent(rv);
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
  public void init(String clusterID, long nextAvailChannelID, Set<ConnectionID> connections) {
    this.uid = clusterID;
    if (nextAvailChannelID >= 0) {
      this.connectionIDSequence.setNext(nextAvailChannelID);
    }
    for (final ConnectionID cid : connections) {
      Assert.assertEquals(clusterID, cid.getServerID());
      this.clientStateStore.saveClientState(new ChannelID(cid.getChannelID()));
    }
  }

  @Override
  public Set<ConnectionID> loadConnectionIDs() {
    Assert.assertNotNull(uid);
    Set<ConnectionID> connections = new HashSet<ConnectionID>();
    for (final ChannelID channelID : clientStateStore.loadClientIDs()) {
      connections.add(new ConnectionID(ConnectionID.NULL_JVM_ID, (channelID).toLong(), uid));
    }
    return connections;
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
  public void channelRemoved(MessageChannel channel) {
    ChannelID clientID = channel.getChannelID();
    try {
      clientStateStore.deleteClientState(clientID);
    } catch (ClientNotFoundException e) {
      throw new AssertionError(e);
    }
    fireDestroyedEvent(new ConnectionID(ConnectionID.NULL_JVM_ID, clientID.toLong(), uid));
  }

  @Override
  public long getCurrentConnectionID() {
    return connectionIDSequence.current();
  }

}