/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactoryListener;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.impl.ClientNotFoundException;
import com.tc.util.Assert;
import com.tc.util.sequence.MutableSequence;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ConnectionIDFactoryImpl implements ConnectionIDFactory, DSOChannelManagerEventListener {

  private final ClientStatePersistor clientStateStore;
  private final MutableSequence   connectionIDSequence;
  private String                     uid;
  private List                       listeners = new CopyOnWriteArrayList();

  public ConnectionIDFactoryImpl(ClientStatePersistor clientStateStore) {
    this.clientStateStore = clientStateStore;
    this.connectionIDSequence = clientStateStore.getConnectionIDSequence();
    this.uid = connectionIDSequence.getUID();
  }

  public ConnectionID nextConnectionId() {
    Assert.assertNotNull(uid);
    long clientID = connectionIDSequence.next();
    // Make sure we save the fact that we are giving out this id to someone in the database before giving it out.
    clientStateStore.saveClientState(new ChannelID(clientID));
    ConnectionID rv = new ConnectionID(clientID, uid);
    fireCreationEvent(rv);
    return rv;
  }

  private void fireCreationEvent(ConnectionID rv) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      ConnectionIDFactoryListener listener = (ConnectionIDFactoryListener) i.next();
      listener.connectionIDCreated(rv);
    }
  }

  private void fireDestroyedEvent(ConnectionID connectionID) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      ConnectionIDFactoryListener listener = (ConnectionIDFactoryListener) i.next();
      listener.connectionIDDestroyed(connectionID);
    }
  }

  public void init(String clusterID, long nextAvailChannelID, Set connections) {
    this.uid = clusterID;
    if (nextAvailChannelID >= 0) {
      this.connectionIDSequence.setNext(nextAvailChannelID);
    }
    for (Iterator i = connections.iterator(); i.hasNext();) {
      ConnectionID cid = (ConnectionID) i.next();
      Assert.assertEquals(clusterID, cid.getServerID());
      this.clientStateStore.saveClientState(new ChannelID(cid.getChannelID()));
    }
  }

  public Set loadConnectionIDs() {
    Assert.assertNotNull(uid);
    Set connections = new HashSet();
    for (Iterator i = clientStateStore.loadClientIDs().iterator(); i.hasNext();) {
      connections.add(new ConnectionID(((ChannelID) i.next()).toLong(), uid));
    }
    return connections;
  }

  public void registerForConnectionIDEvents(ConnectionIDFactoryListener listener) {
    listeners.add(listener);
  }

  public void channelCreated(MessageChannel channel) {
    // NOP
  }

  public void channelRemoved(MessageChannel channel) {
    ChannelID clientID = channel.getChannelID();
    try {
      clientStateStore.deleteClientState(clientID);
    } catch (ClientNotFoundException e) {
      throw new AssertionError(e);
    }
    fireDestroyedEvent(new ConnectionID(clientID.toLong(), uid));
  }

}
