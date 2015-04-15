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
package com.tc.l2.ha;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.objectserver.gtx.GlobalTransactionIDSequenceProvider;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.UUID;
import com.tc.util.sequence.DGCSequenceProvider;
import com.tc.util.sequence.ObjectIDSequence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClusterStateImpl implements ClusterState {

  private static final TCLogger                     logger                 = TCLogging.getLogger(ClusterState.class);

  private final ClusterStatePersistor               clusterStatePersistor;
  private final ObjectIDSequence                    oidSequence;
  private final ConnectionIDFactory                 connectionIdFactory;
  private final GlobalTransactionIDSequenceProvider gidSequenceProvider;
  private final GroupID                             thisGroupID;
  private final StripeIDStateManager                stripeIDStateManager;
  private final DGCSequenceProvider                 dgcSequenceProvider;

  private final Set                                 connections            = Collections.synchronizedSet(new HashSet());
  private long                                      nextAvailObjectID      = -1;
  private long                                      nextAvailChannelID     = -1;
  private long                                      nextAvailGlobalTxnID   = -1;
  private long                                      nextAvailableDGCId     = -1;
  private State                                     currentState;
  private StripeID                                  stripeID;

  private boolean                                   nextObjectIDChanged    = false;
  private boolean                                   nextGlobalTxnIDChanged = false;
  private boolean                                   nextDGCIDChanged       = false;

  public ClusterStateImpl(ClusterStatePersistor clusterStatePersistor, ObjectIDSequence oidSequence,
                      ConnectionIDFactory connectionIdFactory, GlobalTransactionIDSequenceProvider gidSequenceProvider,
                      GroupID thisGroupID, StripeIDStateManager stripeIDStateManager,
                      DGCSequenceProvider dgcSequenceProvider) {
    this.clusterStatePersistor = clusterStatePersistor;
    this.oidSequence = oidSequence;
    this.connectionIdFactory = connectionIdFactory;
    this.gidSequenceProvider = gidSequenceProvider;
    this.thisGroupID = thisGroupID;
    this.stripeIDStateManager = stripeIDStateManager;
    this.dgcSequenceProvider = dgcSequenceProvider;
    this.stripeID = clusterStatePersistor.getThisStripeID();
    this.nextAvailObjectID = this.oidSequence.currentObjectIDValue();
    this.nextAvailGlobalTxnID = this.gidSequenceProvider.currentGID();
    this.nextAvailChannelID = this.connectionIdFactory.getCurrentConnectionID();
    this.nextAvailableDGCId = this.dgcSequenceProvider.currentIDValue();
    checkAndSetGroupID(clusterStatePersistor, thisGroupID);
  }

  private void checkAndSetGroupID(ClusterStatePersistor statePersistor, GroupID groupID) {
    if (statePersistor.getGroupId().isNull()) {
      statePersistor.setGroupId(thisGroupID);
    } else if (!groupID.equals(statePersistor.getGroupId())) {
      logger.error("Found data from the incorrect stripe in the server data path. Verify that the server is starting up " +
                   "with the correct data files and that the cluster topology has not changed across a restart.");
      throw new IllegalStateException("Data for " + statePersistor.getGroupId() + " found. Expected data from group " + thisGroupID + ".");
    }
  }

  @Override
  public void setNextAvailableObjectID(long nextAvailOID) {
    if (nextAvailOID < nextAvailObjectID) {
      // Could happen when two actives fight it out. Dont want to assert, let the state manager fight it out.
      logger.error("Trying to set Next Available ObjectID to a lesser value : known = " + nextAvailObjectID
                   + " new value = " + nextAvailOID + " IGNORING");
      return;
    }
    nextObjectIDChanged = true;
    this.nextAvailObjectID = nextAvailOID;
  }

  @Override
  public long getNextAvailableObjectID() {
    return nextAvailObjectID;
  }

  @Override
  public long getNextAvailableChannelID() {
    return nextAvailChannelID;
  }

  @Override
  public long getNextAvailableGlobalTxnID() {
    return nextAvailGlobalTxnID;
  }

  @Override
  public long getNextAvailableDGCID() {
    return nextAvailableDGCId;
  }

  @Override
  public void setNextAvailableGlobalTransactionID(long nextAvailGID) {
    if (nextAvailGID < nextAvailGlobalTxnID) {
      // Could happen when two actives fight it out. Dont want to assert, let the state manager fight it out.
      logger.error("Trying to set Next Available Global Txn ID to a lesser value : known = " + nextAvailGlobalTxnID
                   + " new value = " + nextAvailGID + " IGNORING");
      return;
    }
    nextGlobalTxnIDChanged = true;
    this.nextAvailGlobalTxnID = nextAvailGID;
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
  }

  @Override
  public void setNextAvailableDGCId(long nextDGCId) {
    if (nextDGCId < this.nextAvailableDGCId) {
      // Could happen when two actives fight it out. Dont want to assert, let the state manager fight it out.
      logger.error("Trying to set Next Available GC ID to a lesser value : known = " + nextAvailableDGCId
                   + " new value = " + nextDGCId + " IGNORING");
      return;
    }
    nextDGCIDChanged = true;
    this.nextAvailableDGCId = nextDGCId;
  }

  @Override
  public void syncActiveState() {
    syncConnectionIDsToDisk();
  }

  @Override
  public void syncSequenceState() {
    if (nextObjectIDChanged) {
      syncOIDSequenceToDisk();
    }
    if (nextGlobalTxnIDChanged) {
      syncGIDSequenceToDisk();
    }
    if (nextDGCIDChanged) {
      syncDGCIDSequenceToDisk();
    }
  }

  private void syncConnectionIDsToDisk() {
    Assert.assertNotNull(stripeID);
    connectionIdFactory.init(stripeID.getName(), nextAvailChannelID, connections);
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
    stripeIDStateManager.verifyOrSaveStripeID(thisGroupID, stripeID, true);
  }

  private void syncStripeIDToDB() {
    clusterStatePersistor.setThisStripeID(stripeID);
  }

  private void syncOIDSequenceToDisk() {
    long nextOID = getNextAvailableObjectID();
    if (nextOID != -1) {
      logger.info("Setting the Next Available OID to " + nextOID);
      this.oidSequence.setNextAvailableObjectID(nextOID);
    }
    nextObjectIDChanged = false;
  }

  private void syncDGCIDSequenceToDisk() {
    logger.info("Setting the next Available DGC sequence to " + this.nextAvailableDGCId);
    this.dgcSequenceProvider.setNextAvailableDGCId(this.nextAvailableDGCId);
    nextDGCIDChanged = false;
  }

  private void syncGIDSequenceToDisk() {
    long nextGID = getNextAvailableGlobalTxnID();
    if (nextGID != -1) {
      logger.info("Setting the Next Available Global Transaction ID to " + nextGID);
      this.gidSequenceProvider.setNextAvailableGID(nextGID);
    }
    nextGlobalTxnIDChanged = false;
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
    }
    connections.add(connID);
  }

  @Override
  public void removeConnection(ConnectionID connectionID) {
    boolean removed = connections.remove(connectionID);
    if (!removed) {
      logger.warn("Connection ID not found : " + connectionID + " Current Connections count : " + connections.size());
    }
  }

  @Override
  public Set getAllConnections() {
    return new HashSet(connections);
  }

  @Override
  public void generateStripeIDIfNeeded() {
    if (isStripeIDNull()) {
      // This is the first time an L2 goes active in the cluster of L2s. Generate a new stripeID. this will stick.
      setStripeID(UUID.getUUID().toString());
    }
  }

  @Override
  public Map<GroupID, StripeID> getStripeIDMap() {
    return stripeIDStateManager.getStripeIDMap(false);
  }

  @Override
  public void addToStripeIDMap(GroupID gid, StripeID sid) {
    stripeIDStateManager.verifyOrSaveStripeID(gid, sid, true);
  }

  @Override
  public String toString() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("ClusterState [ ");
    strBuilder.append("Connections [ ").append(this.connections).append(" ]");
    strBuilder.append(" nextAvailGlobalTxnID: ").append(this.nextAvailGlobalTxnID);
    strBuilder.append(" nextAvailChannelID: ").append(this.nextAvailChannelID);
    strBuilder.append(" nextAvailObjectID: ").append(this.nextAvailObjectID);
    strBuilder.append(" nextAvailDGCSequence: ").append(this.nextAvailableDGCId);
    strBuilder.append(" currentState: ").append(this.currentState);
    strBuilder.append(" stripeID: ").append(this.stripeID);
    strBuilder.append(" ]");
    return strBuilder.toString();
  }

}
