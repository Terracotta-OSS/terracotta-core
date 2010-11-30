/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import static com.tc.l2.ha.ClusterStateDBKeyNames.CLUSTER_ID_KEY;
import static com.tc.l2.ha.ClusterStateDBKeyNames.L2_STATE_KEY;

import com.tc.exception.CleanDirtyDatabaseException;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.TerracottaOperatorEventLogger;
import com.tc.logging.TerracottaOperatorEventLogging;
import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.gtx.GlobalTransactionIDSequenceProvider;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.text.Banner;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.UUID;
import com.tc.util.sequence.DGCSequenceProvider;
import com.tc.util.sequence.ObjectIDSequence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClusterState {

  private static final TCLogger                     logger               = TCLogging.getLogger(ClusterState.class);

  private final PersistentMapStore                  persistentStateStore;
  private final ObjectIDSequence                    oidSequence;
  private final ConnectionIDFactory                 connectionIdFactory;
  private final GlobalTransactionIDSequenceProvider gidSequenceProvider;
  private final GroupID                             thisGroupID;
  private final StripeIDStateManager                stripeIDStateManager;
  private final DGCSequenceProvider                 dgcSequenceProvider;

  private final Set                                 connections          = Collections.synchronizedSet(new HashSet());
  private long                                      nextAvailObjectID    = -1;
  private long                                      nextAvailChannelID   = -1;
  private long                                      nextAvailGlobalTxnID = -1;
  private long                                      nextAvailableDGCId   = -1;
  private State                                     currentState;
  private StripeID                                  stripeID;
  private final TerracottaOperatorEventLogger       operatorEventLogger  = TerracottaOperatorEventLogging
                                                                             .getEventLogger();

  public ClusterState(PersistentMapStore persistentStateStore, ObjectIDSequence oidSequence,
                      ConnectionIDFactory connectionIdFactory, GlobalTransactionIDSequenceProvider gidSequenceProvider,
                      GroupID thisGroupID, StripeIDStateManager stripeIDStateManager,
                      DGCSequenceProvider dgcSequenceProvider) {
    this.persistentStateStore = persistentStateStore;
    this.oidSequence = oidSequence;
    this.connectionIdFactory = connectionIdFactory;
    this.gidSequenceProvider = gidSequenceProvider;
    this.thisGroupID = thisGroupID;
    this.stripeIDStateManager = stripeIDStateManager;
    this.dgcSequenceProvider = dgcSequenceProvider;
    String sid = persistentStateStore.get(CLUSTER_ID_KEY);
    this.stripeID = (sid != null) ? new StripeID(sid) : StripeID.NULL_ID;
    validateStartupState(persistentStateStore.get(L2_STATE_KEY));
    this.nextAvailObjectID = this.oidSequence.currentObjectIDValue();
    this.nextAvailGlobalTxnID = this.gidSequenceProvider.currentGID();
    this.nextAvailChannelID = this.connectionIdFactory.getCurrentConnectionID();
    this.nextAvailableDGCId = this.dgcSequenceProvider.currentIDValue();
  }

  private void validateStartupState(String stateStr) {
    if (stateStr != null) {
      State stateB4Crash = new State(stateStr);
      if (!StateManager.ACTIVE_COORDINATOR.equals(stateB4Crash)) {
        /*
         * The server is running in persistent mode and this instance of the server was not the ACTIVE server before the
         * crash. Force user to clean up the DB so that this server can resync state from the ACTIVE.
         */
        String errorMessage = Banner
            .makeBanner("This server is running with persistence turned on and was stopped in " + stateStr
                        + " state. Only the " + StateManager.ACTIVE_COORDINATOR.getName() + " server is allowed "
                        + " to be restarted without cleaning up the data directory with persistence turned on.\n\n"
                        + "Please clean up the data directory and make sure that the "
                        + StateManager.ACTIVE_COORDINATOR.getName()
                        + " is up and running before starting this server. It is important that the "
                        + StateManager.ACTIVE_COORDINATOR.getName()
                        + " is up and running before starting this server else you might end up losing data", "ERROR");
        logger.error(errorMessage, new Throwable());
        operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createDirtyDBEvent());
        throw new CleanDirtyDatabaseException(errorMessage);
      }
    }
  }

  public void setNextAvailableObjectID(long nextAvailOID) {
    if (nextAvailOID < nextAvailObjectID) {
      // Could happen when two actives fight it out. Dont want to assert, let the state manager fight it out.
      logger.error("Trying to set Next Available ObjectID to a lesser value : known = " + nextAvailObjectID
                   + " new value = " + nextAvailOID + " IGNORING");
      return;
    }
    this.nextAvailObjectID = nextAvailOID;
  }

  public long getNextAvailableObjectID() {
    return nextAvailObjectID;
  }

  public long getNextAvailableChannelID() {
    return nextAvailChannelID;
  }

  public long getNextAvailableGlobalTxnID() {
    return nextAvailGlobalTxnID;
  }

  public long getNextAvailableDGCID() {
    return nextAvailableDGCId;
  }

  public void setNextAvailableGlobalTransactionID(long nextAvailGID) {
    if (nextAvailGID < nextAvailGlobalTxnID) {
      // Could happen when two actives fight it out. Dont want to assert, let the state manager fight it out.
      logger.error("Trying to set Next Available Global Txn ID to a lesser value : known = " + nextAvailGlobalTxnID
                   + " new value = " + nextAvailGID + " IGNORING");
      return;
    }
    this.nextAvailGlobalTxnID = nextAvailGID;
  }

  public void setNextAvailableChannelID(long nextAvailableCID) {
    if (nextAvailableCID < nextAvailChannelID) {
      // Could happen when two actives fight it out. Dont want to assert, let the state manager fight it out.
      logger.error("Trying to set Next Available ChannelID to a lesser value : known = " + nextAvailChannelID
                   + " new value = " + nextAvailableCID + " IGNORING");
      return;
    }
    this.nextAvailChannelID = nextAvailableCID;
  }

  public void setNextAvailableDGCId(long nextDGCId) {
    if (nextDGCId < this.nextAvailableDGCId) {
      // Could happen when two actives fight it out. Dont want to assert, let the state manager fight it out.
      logger.error("Trying to set Next Available GC ID to a lesser value : known = " + nextAvailableDGCId
                   + " new value = " + nextDGCId + " IGNORING");
      return;
    }
    this.nextAvailableDGCId = nextDGCId;
  }

  public void syncInternal() {
    syncConnectionIDsToDisk();
    syncOIDSequenceToDisk();
    syncGIDSequenceToDisk();
    syncDGCIDSequenceToDisk();
  }

  private void syncConnectionIDsToDisk() {
    Assert.assertNotNull(stripeID);
    connectionIdFactory.init(stripeID.getName(), nextAvailChannelID, connections);
  }

  public StripeID getStripeID() {
    return stripeID;
  }

  public boolean isStripeIDNull() {
    return stripeID.isNull();
  }

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
    persistentStateStore.put(CLUSTER_ID_KEY, stripeID.getName());
  }

  private void syncOIDSequenceToDisk() {
    long nextOID = getNextAvailableObjectID();
    if (nextOID != -1) {
      logger.info("Setting the Next Available OID to " + nextOID);
      this.oidSequence.setNextAvailableObjectID(nextOID);
    }
  }

  private void syncDGCIDSequenceToDisk() {
    logger.info("Setting the next Available DGC sequence to " + this.nextAvailableDGCId);
    this.dgcSequenceProvider.setNextAvailableDGCId(this.nextAvailableDGCId);
  }

  private void syncGIDSequenceToDisk() {
    long nextGID = getNextAvailableGlobalTxnID();
    if (nextGID != -1) {
      logger.info("Setting the Next Available Global Transaction ID to " + nextGID);
      this.gidSequenceProvider.setNextAvailableGID(nextGID);
    }
  }

  public void setCurrentState(State state) {
    this.currentState = state;
    syncCurrentStateToDB();
  }

  private void syncCurrentStateToDB() {
    persistentStateStore.put(L2_STATE_KEY, currentState.getName());
  }

  public void addNewConnection(ConnectionID connID) {
    if (connID.getChannelID() >= nextAvailChannelID) {
      nextAvailChannelID = connID.getChannelID() + 1;
    }
    connections.add(connID);
  }

  public void removeConnection(ConnectionID connectionID) {
    boolean removed = connections.remove(connectionID);
    if (!removed) {
      logger.warn("Connection ID not found : " + connectionID + " Current Connections count : " + connections.size());
    }
  }

  public Set getAllConnections() {
    return new HashSet(connections);
  }

  public static final String getL2StateKey() {
    return L2_STATE_KEY;
  }

  public void generateStripeIDIfNeeded() {
    if (isStripeIDNull()) {
      // This is the first time an L2 goes active in the cluster of L2s. Generate a new stripeID. this will stick.
      setStripeID(UUID.getUUID().toString());
    }
    syncInternal();
  }

  public Map<GroupID, StripeID> getStripeIDMap() {
    return stripeIDStateManager.getStripeIDMap(false);
  }

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
