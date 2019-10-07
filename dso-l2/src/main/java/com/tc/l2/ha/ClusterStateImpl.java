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
package com.tc.l2.ha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.StripeID;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.objectserver.api.ClientNotFoundException;
import com.tc.objectserver.persistence.Persistor;
import com.tc.util.State;
import com.tc.util.UUID;
import com.terracotta.config.ConfigurationProvider;

import java.util.ArrayList;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClusterStateImpl implements ClusterState {

  private static final Logger logger = LoggerFactory.getLogger(ClusterState.class);

  private final Persistor               persistor;
  private final ConnectionIDFactory                 connectionIdFactory;
  private final StripeIDStateManager                stripeIDStateManager;

  private final Set<ConnectionID>                   connections            = Collections.synchronizedSet(new HashSet<ConnectionID>());
  private long                                      nextAvailChannelID     = -1;
  private State                                     currentState;
  private byte[]                                    configuration = new byte[0];

  public ClusterStateImpl(Persistor persistor, 
                          ConnectionIDFactory connectionIdFactory, StripeIDStateManager stripeIDStateManager) {
    this.persistor = persistor;
    this.connectionIdFactory = connectionIdFactory;
    this.stripeIDStateManager = stripeIDStateManager;
    this.nextAvailChannelID = this.connectionIdFactory.getCurrentConnectionID();
  }

  @Override
  public long getNextAvailableChannelID() {
    return nextAvailChannelID;
  }

  @Override
  public void setNextAvailableChannelID(long nextAvailableCID) {
    if (nextAvailableCID < nextAvailChannelID) {
      // Could happen when two actives fight it out. Dont want to assert, let the state manager fight it out.
      logger.error("Trying to set Next Available ChannelID to a lesser value : known = " + nextAvailChannelID
                   + " new value = " + nextAvailableCID + " IGNORING");
      return;
    }
    this.nextAvailChannelID = nextAvailableCID;
    persistor.getClientStatePersistor().getConnectionIDSequence().setNext(nextAvailChannelID);
  }

  @Override
  public void syncActiveState() {
// activate the connection id factory so that it can be used to create connection ids
// this happens for active only
// when going active, start the next available ID+10 so that on restarts with persistent state, 
// this active is picked via the additional election weightings
    setNextAvailableChannelID(nextAvailChannelID + 10);
    connectionIdFactory.activate(stripeIDStateManager.getStripeID(), nextAvailChannelID);
  }

  @Override
  public void syncSequenceState() {
  }

  @Override
  public StripeID getStripeID() {
    return stripeIDStateManager.getStripeID();
  }

  private boolean isStripeIDNull() {
    return stripeIDStateManager.getStripeID().isNull();
  }

  @Override
  public void setStripeID(String uid) {
    if (!isStripeIDNull() && !stripeIDStateManager.getStripeID().getName().equals(uid)) {
      logger.error("StripeID doesnt match !! Mine : " + stripeIDStateManager.getStripeID() + " Active sent clusterID as : " + uid);
      throw new ClusterIDMissmatchException(stripeIDStateManager.getStripeID().getName(), uid);
    }

    // notify stripeIDStateManager
    stripeIDStateManager.verifyOrSaveStripeID(new StripeID(uid), true);
  }

  @Override
  public void setCurrentState(State state) {
    this.currentState = state;
    syncCurrentStateToDB();
  }

  private void syncCurrentStateToDB() {
    persistor.getClusterStatePersistor().setCurrentL2State(currentState);
  }

  @Override
  public void addNewConnection(ConnectionID connID) {
    if (connID.getChannelID() >= nextAvailChannelID) {
      nextAvailChannelID = connID.getChannelID() + 1;
      persistor.getClientStatePersistor().getConnectionIDSequence().setNext(nextAvailChannelID);
    }
    connections.add(connID);
    if (connID.getProductId().isReconnectEnabled()) {
      persistor.addClientState(connID.getClientID(), connID.getProductId());
    }
  }

  @Override
  public void removeConnection(ConnectionID connectionID) {
    boolean removed = connections.remove(connectionID);
    if (!removed) {
      logger.debug("Connection ID not found, must be a failed reconnect : " + connectionID + " Current Connections count : " + connections.size());
    }
    try {
      if (connectionID.getProductId().isReconnectEnabled()) {
        persistor.removeClientState(connectionID.getClientID());
      }
    } catch (ClientNotFoundException notfound) {
      logger.debug("not found", notfound);
    }
  }

  @Override
  public Set<ConnectionID> getAllConnections() {
    return new HashSet<>(connections);
  }

  @Override
  public void generateStripeIDIfNeeded() {
    if (isStripeIDNull()) {
      // This is the first time an L2 goes active in the cluster of L2s. Generate a new stripeID. this will stick.
      setStripeID(UUID.getUUID().toString());
    }
  }
  
  @Override
  public String toString() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("ClusterState [ ");
    strBuilder.append("Connections [ ").append(this.connections).append(" ]");
    strBuilder.append(" nextAvailChannelID: ").append(this.nextAvailChannelID);
    strBuilder.append(" currentState: ").append(this.currentState);
    strBuilder.append(" stripeID: ").append(stripeIDStateManager.getStripeID());
    strBuilder.append(" ]");
    return strBuilder.toString();
  }

  @Override
  public void reportStateToMap(Map<String, Object> state) {
    List<String> connects = new ArrayList<>(this.connections.size());
    this.connections.forEach((c)->connects.add(c.toString()));
    state.put("connections", connects);
    state.put("nextChannelID", this.nextAvailChannelID);
    state.put("currentState", this.currentState);
    state.put("stripeID", stripeIDStateManager.getStripeID());
  }

  @Override
  public byte[] getConfiguration() {
    return configuration;
  }

  @Override
  public void setConfiguration(byte[] configuration) {
    this.configuration = configuration;
  }
}
