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

import junit.framework.TestCase;

public class ClusterStateMessageTest extends TestCase {

  private ClusterState clusterState;

  public void setUp() {
    resetClusterState();
  }

  public void tearDown() {
    clusterState = null;
  }

  private void resetClusterState() {
    Persistor persistor = new InMemoryPersistor();
    PersistentMapStore clusterStateStore = persistor.getClusterStateStore();
    ObjectIDSequence oidSequence = new PersistentManagedObjectStore(new TestManagedObjectPersistor(new HashMap()));
    ConnectionIDFactory connectionIdFactory = new ConnectionIDFactoryImpl(persistor.getClientStatePersistor());
    GlobalTransactionIDSequenceProvider gidSequenceProvider = new GlobalTransactionIDBatchRequestHandler(
                                                                                                         new TestMutableSequence());
    clusterState = new ClusterState(clusterStateStore, oidSequence, connectionIdFactory, gidSequenceProvider);
    clusterState.setClusterID("foobar");
  }

  private void validate(ClusterStateMessage csm, ClusterStateMessage csm1) {
    assertEquals(csm.getNextAvailableObjectID(), csm1.getNextAvailableObjectID());
    assertEquals(csm.getNextAvailableGlobalTxnID(), csm1.getNextAvailableGlobalTxnID());
    assertEquals(csm.getClusterID(), csm1.getClusterID());
    assertEquals(csm.getConnectionID(), csm1.getConnectionID());
    assertEquals(csm.getMessageID(), csm1.getMessageID());
    assertEquals(csm.getType(), csm1.getType());
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

  private void addConnectionIDs() {
    ConnectionID connectionID_1 = new ConnectionID(10, "foo");
    ConnectionID connectionID_2 = new ConnectionID(11, "foo");
    ConnectionID connectionID_3 = new ConnectionID(12, "goo");

    clusterState.addNewConnection(connectionID_1);
    clusterState.addNewConnection(connectionID_2);
    clusterState.addNewConnection(connectionID_3);
  }

  private void modifyClusterState() {
    addConnectionIDs();
    ConnectionID connectionID = new ConnectionID(11, "foo");
    clusterState.removeConnection(connectionID);

    clusterState.setNextAvailableChannelID(13);
    clusterState.setNextAvailableObjectID(222);
    clusterState.setNextAvailableGlobalTransactionID(333);
    clusterState.setCurrentState(new State("testing"));
  }

  public void testBasicSerialization() throws Exception {
    modifyClusterState();

    ClusterStateMessage csm = (ClusterStateMessage) ClusterStateMessageFactory.createClusterStateMessage(clusterState);
    ClusterStateMessage csm1 = writeAndRead(csm);
    validate(csm, csm1);

    csm = (ClusterStateMessage) ClusterStateMessageFactory.createOKResponse(new ClusterStateMessage());
    csm1 = writeAndRead(csm);
    validate(csm, csm1);
  }

  public void testInitState() throws Exception {
    ConnectionID connectionID = new ConnectionID(10, "foo");

    // COMPLETE_STATE
    modifyClusterState();
    ClusterStateMessage csm = (ClusterStateMessage) ClusterStateMessageFactory.createClusterStateMessage(clusterState);
    ClusterStateMessage csm1 = writeAndRead(csm);
    validate(csm, csm1);
    resetClusterState();
    csm1.initState(clusterState);
    csm = (ClusterStateMessage) ClusterStateMessageFactory.createClusterStateMessage(clusterState);
    csm1 = writeAndRead(csm);
    validate(csm, csm1);

    // GLOBAL_TRANSACTION_ID
    resetClusterState();
    modifyClusterState();
    csm = (ClusterStateMessage) ClusterStateMessageFactory.createNextAvailableGlobalTransactionIDMessage(clusterState);
    csm1 = writeAndRead(csm);
    validate(csm, csm1);
    resetClusterState();
    csm1.initState(clusterState);
    csm = (ClusterStateMessage) ClusterStateMessageFactory.createNextAvailableGlobalTransactionIDMessage(clusterState);
    csm1 = writeAndRead(csm);
    validate(csm, csm1);

    // OBJECT_ID
    resetClusterState();
    modifyClusterState();
    csm = (ClusterStateMessage) ClusterStateMessageFactory.createNextAvailableObjectIDMessage(clusterState);
    csm1 = writeAndRead(csm);
    validate(csm, csm1);
    resetClusterState();
    csm1.initState(clusterState);
    csm = (ClusterStateMessage) ClusterStateMessageFactory.createNextAvailableObjectIDMessage(clusterState);
    csm1 = writeAndRead(csm);
    validate(csm, csm1);

    // NEW_CONNECTION_CREATED
    resetClusterState();
    csm = (ClusterStateMessage) ClusterStateMessageFactory.createNewConnectionCreatedMessage(connectionID);
    csm1 = writeAndRead(csm);
    validate(csm, csm1);
    resetClusterState();
    csm1.initState(clusterState);
    assertEquals(connectionID, clusterState.getAllConnections().iterator().next());

    // CONNECTION_DESTROYED
    resetClusterState();
    csm = (ClusterStateMessage) ClusterStateMessageFactory.createConnectionDestroyedMessage(connectionID);
    csm1 = writeAndRead(csm);
    validate(csm, csm1);
    clusterState.addNewConnection(connectionID);
    csm1.initState(clusterState);
    assertEquals(0, clusterState.getAllConnections().size());
  }

}
