/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.objectserver.gtx.GlobalTransactionIDSequenceProvider;
import com.tc.objectserver.persistence.api.PersistentMapStore;
import com.tc.text.Banner;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.sequence.ObjectIDSequence;

import java.util.HashSet;
import java.util.Set;

public class ClusterState {

  private static final TCLogger                     logger               = TCLogging.getLogger(ClusterState.class);

  private static final String                       L2_STATE_KEY         = "CLUSTER_STATE::L2_STATE_KEY";
  private static final String                       CLUSTER_ID_KEY       = "CLUSTER_STATE::CLUSTER_ID_KEY";

  private final PersistentMapStore                  clusterStateStore;
  private final ObjectIDSequence                    oidSequence;
  private final ConnectionIDFactory                 connectionIdFactory;
  private final GlobalTransactionIDSequenceProvider gidSequenceProvider;

  private final Set                                 connections          = new HashSet();
  private long                                      nextAvailObjectID    = -1;
  private long                                      nextAvailChannelID   = -1;
  private long                                      nextAvailGlobalTxnID = -1;
  private State                                     currentState;
  private String                                    clusterID;

  public ClusterState(PersistentMapStore clusterStateStore, ObjectIDSequence oidSequence,
                      ConnectionIDFactory connectionIdFactory, GlobalTransactionIDSequenceProvider gidSequenceProvider) {
    this.clusterStateStore = clusterStateStore;
    this.oidSequence = oidSequence;
    this.connectionIdFactory = connectionIdFactory;
    this.gidSequenceProvider = gidSequenceProvider;
    this.clusterID = clusterStateStore.get(CLUSTER_ID_KEY);
    validateStartupState(clusterStateStore.get(L2_STATE_KEY));
  }

  private void validateStartupState(String stateStr) {
    if (stateStr != null) {
      State stateB4Crash = new State(stateStr);
      if (!StateManager.ACTIVE_COORDINATOR.equals(stateB4Crash)) {
        /*
         * The server is running in persisitent mode and this instance of the server was not the ACTIVE server before
         * the crash. Force user to clean up the DB so that this server can resync state from the ACTIVE.
         */
        throw new AssertionError(Banner
            .makeBanner("This server is running with persistence turned on and was stopped in " + stateStr
                        + " state. Only the " + StateManager.ACTIVE_COORDINATOR.getName() + " server is allowed "
                        + " to be restarted without cleaning up the data directory with persistence turned on.\n\n"
                        + "Please clean up the data directory and make sure that the "
                        + StateManager.ACTIVE_COORDINATOR.getName()
                        + " is up and running before starting this server. It is important that the "
                        + StateManager.ACTIVE_COORDINATOR.getName()
                        + " is up and running before starting this server else you might end up losing data", "ERROR"));
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

  public void syncInternal() {
    syncConnectionIDs();
    syncOIDSequence();
    syncGIDSequence();
  }

  private void syncConnectionIDs() {
    Assert.assertNotNull(clusterID);
    connectionIdFactory.init(clusterID, nextAvailChannelID, connections);
  }

  public String getClusterID() {
    return clusterID;
  }

  public void setClusterID(String uid) {
    if (clusterID != null && !clusterID.equals(uid)) {
      logger.error("Cluster ID doesnt match !! Mine : " + clusterID + " Active sent clusterID as : " + uid);
      throw new AssertionError("ClusterIDs dont match : " + clusterID + " <-> " + uid);
    }
    clusterID = uid;
    synchClusterIDToDB();
  }

  private void synchClusterIDToDB() {
    clusterStateStore.put(CLUSTER_ID_KEY, clusterID);
  }

  private void syncOIDSequence() {
    long nextOID = getNextAvailableObjectID();
    if (nextOID != -1) {
      logger.info("Setting the Next Available OID to " + nextOID);
      this.oidSequence.setNextAvailableObjectID(nextOID);
    }
  }

  private void syncGIDSequence() {
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
    clusterStateStore.put(L2_STATE_KEY, currentState.getName());
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
}
