/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.ha.ClusterState;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.objectserver.gtx.GlobalTransactionIDSequenceProvider;
import com.tc.objectserver.handler.GlobalTransactionIDBatchRequestHandler;
import com.tc.objectserver.impl.PersistentManagedObjectStore;
import com.tc.objectserver.impl.TestManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistentMapStore;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.impl.InMemoryPersistor;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.objectserver.persistence.sleepycat.ConnectionIDFactoryImpl;
import com.tc.util.State;
import com.tc.util.sequence.ObjectIDSequence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;

import junit.framework.TestCase;

public class ClusterStateMessageTest extends TestCase {
  private static final int CLUSTER_STATE_1 = 1;
  private static final int CLUSTER_STATE_2 = 2;

  private ClusterState     clusterState_1;
  private ClusterState     clusterState_2;

  public void setUp() {
    resetClusterState(CLUSTER_STATE_1);
    resetClusterState(CLUSTER_STATE_2);
  }

  public void tearDown() {
    clusterState_1 = null;
    clusterState_2 = null;
  }

  private void resetClusterState(int clusterState) {
    Persistor persistor = new InMemoryPersistor();
    PersistentMapStore clusterStateStore = persistor.getClusterStateStore();
    ObjectIDSequence oidSequence = new PersistentManagedObjectStore(new TestManagedObjectPersistor(new HashMap()));
    ConnectionIDFactory connectionIdFactory = new ConnectionIDFactoryImpl(persistor.getClientStatePersistor());
    GlobalTransactionIDSequenceProvider gidSequenceProvider = new GlobalTransactionIDBatchRequestHandler(
                                                                                                         new TestMutableSequence());
    if (clusterState == CLUSTER_STATE_1) {
      clusterState_1 = new ClusterState(clusterStateStore, oidSequence, connectionIdFactory, gidSequenceProvider);
      clusterState_1.setClusterID("foobar");
    } else {
      clusterState_2 = new ClusterState(clusterStateStore, oidSequence, connectionIdFactory, gidSequenceProvider);
      clusterState_2.setClusterID("foobar");
    }
  }

  private void validate(ClusterStateMessage csm, ClusterStateMessage csm1) {
    assertEquals(csm.getNextAvailableObjectID(), csm1.getNextAvailableObjectID());
    assertEquals(csm.getNextAvailableGlobalTxnID(), csm1.getNextAvailableGlobalTxnID());
    assertEquals(csm.getClusterID(), csm1.getClusterID());
    assertEquals(csm.getConnectionID(), csm1.getConnectionID());
    assertEquals(csm.getMessageID(), csm1.getMessageID());
    assertEquals(csm.getType(), csm1.getType());
  }

  private void validate(ClusterState cs, ClusterState cs1) {
    assertEquals(cs.getNextAvailableChannelID(), cs1.getNextAvailableChannelID());
    assertEquals(cs.getNextAvailableGlobalTxnID(), cs1.getNextAvailableGlobalTxnID());
    assertEquals(cs.getNextAvailableObjectID(), cs1.getNextAvailableObjectID());
    assertEquals(cs.getAllConnections().size(), cs1.getAllConnections().size());

    Iterator iter1 = cs1.getAllConnections().iterator();
    for (Iterator iter = cs.getAllConnections().iterator(); iter.hasNext();) {
      assertEquals(((ConnectionID) iter.next()).getID(), ((ConnectionID) iter1.next()).getID());
    }

    assertEquals(cs.getClusterID(), cs1.getClusterID());
  }

  private ClusterStateMessage writeAndRead(ClusterStateMessage csm) throws Exception {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput oo = new ObjectOutputStream(bo);
    oo.writeObject(csm);
    System.err.println("Written : " + csm);
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInput oi = new ObjectInputStream(bi);
    ClusterStateMessage csm1 = (ClusterStateMessage) oi.readObject();
    System.err.println("Read : " + csm1);
    return csm1;
  }

  private void modifyClusterState(int clusterState) {
    ConnectionID connectionID_1 = new ConnectionID(10, "foo");
    ConnectionID connectionID_2 = new ConnectionID(11, "foo");
    ConnectionID connectionID_3 = new ConnectionID(12, "goo");

    if (clusterState == CLUSTER_STATE_1) {
      clusterState_1.addNewConnection(connectionID_1);
      clusterState_1.addNewConnection(connectionID_2);
      clusterState_1.addNewConnection(connectionID_3);
      clusterState_1.removeConnection(connectionID_2);
      clusterState_1.setNextAvailableChannelID(13);
      clusterState_1.setNextAvailableObjectID(222);
      clusterState_1.setNextAvailableGlobalTransactionID(333);
      clusterState_1.setCurrentState(new State("testing"));
    } else {
      clusterState_2.addNewConnection(connectionID_1);
      clusterState_2.addNewConnection(connectionID_2);
      clusterState_2.addNewConnection(connectionID_3);
      clusterState_2.removeConnection(connectionID_2);
      clusterState_2.setNextAvailableChannelID(13);
      clusterState_2.setNextAvailableObjectID(222);
      clusterState_2.setNextAvailableGlobalTransactionID(333);
      clusterState_2.setCurrentState(new State("testing"));

    }
  }

  public void testBasicSerialization() throws Exception {
    modifyClusterState(CLUSTER_STATE_1);

    ClusterStateMessage csm = (ClusterStateMessage) ClusterStateMessageFactory
        .createClusterStateMessage(clusterState_1);
    ClusterStateMessage csm1 = writeAndRead(csm);
    validate(csm, csm1);

    csm = (ClusterStateMessage) ClusterStateMessageFactory.createOKResponse(new ClusterStateMessage());
    csm1 = writeAndRead(csm);
    validate(csm, csm1);
  }

  public void testInitState() throws Exception {
    ConnectionID connectionID = new ConnectionID(10, "foo");

    // COMPLETE_STATE
    modifyClusterState(CLUSTER_STATE_1);
    ClusterStateMessage csm = (ClusterStateMessage) ClusterStateMessageFactory
        .createClusterStateMessage(clusterState_1);
    ClusterStateMessage csm1 = writeAndRead(csm);
    validate(csm, csm1);
    csm1.initState(clusterState_2);
    validate(clusterState_1, clusterState_2);

    // GLOBAL_TRANSACTION_ID
    resetClusterState(CLUSTER_STATE_1);
    resetClusterState(CLUSTER_STATE_2);
    clusterState_1.setNextAvailableGlobalTransactionID(3423);
    csm = (ClusterStateMessage) ClusterStateMessageFactory
        .createNextAvailableGlobalTransactionIDMessage(clusterState_1);
    csm1 = writeAndRead(csm);
    validate(csm, csm1);
    csm1.initState(clusterState_2);
    validate(clusterState_1, clusterState_2);

    // OBJECT_ID
    resetClusterState(CLUSTER_STATE_1);
    resetClusterState(CLUSTER_STATE_2);
    clusterState_1.setNextAvailableObjectID(6868);
    csm = (ClusterStateMessage) ClusterStateMessageFactory.createNextAvailableObjectIDMessage(clusterState_1);
    csm1 = writeAndRead(csm);
    validate(csm, csm1);
    csm1.initState(clusterState_2);
    validate(clusterState_1, clusterState_2);

    // NEW_CONNECTION_CREATED
    csm = (ClusterStateMessage) ClusterStateMessageFactory.createNewConnectionCreatedMessage(connectionID);
    csm1 = writeAndRead(csm);
    validate(csm, csm1);
    resetClusterState(CLUSTER_STATE_2);
    csm1.initState(clusterState_2);
    assertEquals(connectionID, clusterState_2.getAllConnections().iterator().next());

    // CONNECTION_DESTROYED
    csm = (ClusterStateMessage) ClusterStateMessageFactory.createConnectionDestroyedMessage(connectionID);
    csm1 = writeAndRead(csm);
    validate(csm, csm1);
    resetClusterState(CLUSTER_STATE_2);
    clusterState_2.addNewConnection(connectionID);
    csm1.initState(clusterState_2);
    assertEquals(0, clusterState_2.getAllConnections().size());
  }

}
