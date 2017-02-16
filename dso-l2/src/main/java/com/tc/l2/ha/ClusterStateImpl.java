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

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.StripeID;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.objectserver.api.ClientNotFoundException;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.util.State;
import com.tc.util.UUID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClusterStateImpl implements ClusterState {

  private static final TCLogger                     logger                 = TCLogging.getLogger(ClusterState.class);

  private final ClusterStatePersistor               clusterStatePersistor;
  private final ClientStatePersistor                clientStatePersistor;
  private final ConnectionIDFactory                 connectionIdFactory;
  private final StripeIDStateManager                stripeIDStateManager;

  private final Set<ConnectionID>                   connections            = Collections.synchronizedSet(new HashSet<ConnectionID>());
  private long                                      nextAvailChannelID     = -1;
  private State                                     currentState;
  private StripeID                                  stripeID;

  public ClusterStateImpl(ClusterStatePersistor clusterStatePersistor, ClientStatePersistor clientStatePersistor, 
                          ConnectionIDFactory connectionIdFactory, StripeIDStateManager stripeIDStateManager) {
    this.clusterStatePersistor = clusterStatePersistor;
    this.clientStatePersistor = clientStatePersistor;
    this.connectionIdFactory = connectionIdFactory;
    this.stripeIDStateManager = stripeIDStateManager;
    this.stripeID = clusterStatePersistor.getThisStripeID();
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
    clientStatePersistor.getConnectionIDSequence().setNext(nextAvailChannelID);
  }

  @Override
  public void syncActiveState() {
// activate the connection id factory so that it can be used to create connection ids
// this happens for active only
    connectionIdFactory.activate(stripeID, nextAvailChannelID);
  }

  @Override
  public void syncSequenceState() {
  }

  @Override
  public StripeID getStripeID() {
    return stripeID;
  }

  public boolean isStripeIDNull() {
    return stripeID.isNull();
  }

  @Override
  public void setStripeID(String uid) {
    if (!isStripeIDNull() && !stripeID.getName().equals(uid)) {
      logger.error("StripeID doesnt match !! Mine : " + stripeID + " Active sent clusterID as : " + uid);
      throw new ClusterIDMissmatchException(stripeID.getName(), uid);
    }
    stripeID = new StripeID(uid);
    syncStripeIDToDB();

    // notify stripeIDStateManager
    stripeIDStateManager.verifyOrSaveStripeID(stripeID, true);
  }

  private void syncStripeIDToDB() {
    clusterStatePersistor.setThisStripeID(stripeID);
  }

  @Override
  public void setCurrentState(State state) {
    this.currentState = state;
    syncCurrentStateToDB();
  }

  private void syncCurrentStateToDB() {
    clusterStatePersistor.setCurrentL2State(currentState);
  }

  @Override
  public void addNewConnection(ConnectionID connID) {
    if (connID.getChannelID() >= nextAvailChannelID) {
      nextAvailChannelID = connID.getChannelID() + 1;
      clientStatePersistor.getConnectionIDSequence().setNext(nextAvailChannelID);
    }
    connections.add(connID);
    clientStatePersistor.saveClientState(connID.getClientID());
  }

  @Override
  public void removeConnection(ConnectionID connectionID) {
    boolean removed = connections.remove(connectionID);
    if (!removed) {
      logger.warn("Connection ID not found : " + connectionID + " Current Connections count : " + connections.size());
    } else {
      try {
        clientStatePersistor.deleteClientState(connectionID.getClientID());
      } catch (ClientNotFoundException notfound) {
        logger.warn("not found", notfound);
      }
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
    strBuilder.append(" stripeID: ").append(this.stripeID);
    strBuilder.append(" ]");
    return strBuilder.toString();
  }

}
