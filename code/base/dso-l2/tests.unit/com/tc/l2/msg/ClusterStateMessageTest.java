/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.ha.ClusterState;
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
import com.tc.util.UUID;
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
  public void testSerialization() throws Exception {

    Persistor persistor = new InMemoryPersistor();
    PersistentMapStore clusterStateStore = persistor.getClusterStateStore();
    ObjectIDSequence oidSequence = new PersistentManagedObjectStore(new TestManagedObjectPersistor(new HashMap()));
    ConnectionIDFactory connectionIdFactory = new ConnectionIDFactoryImpl(persistor.getClientStatePersistor());
    GlobalTransactionIDSequenceProvider gidSequenceProvider = new GlobalTransactionIDBatchRequestHandler(
                                                                                                         new TestMutableSequence());
    ClusterState clusterState = new ClusterState(clusterStateStore, oidSequence, connectionIdFactory,
                                                 gidSequenceProvider);
    clusterState.setClusterID(UUID.getUUID().toString());
    ClusterStateMessage csm = (ClusterStateMessage) ClusterStateMessageFactory.createClusterStateMessage(clusterState);
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput oo = new ObjectOutputStream(bo);
    oo.writeObject(csm);
    System.err.println("Written : " + csm);
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInput oi = new ObjectInputStream(bi);
    ClusterStateMessage csm1 = (ClusterStateMessage) oi.readObject();
    System.err.println("Read : " + csm1);

    assertEquals(csm.getNextAvailableObjectID(), csm1.getNextAvailableGlobalTxnID());
    assertEquals(csm.getNextAvailableGlobalTxnID(), csm1.getNextAvailableGlobalTxnID());
    assertEquals(csm.getClusterID(), csm1.getClusterID());
    assertEquals(csm.getConnectionID(), csm1.getConnectionID());
    assertEquals(csm.getMessageID(), csm1.getMessageID());
    assertEquals(csm.getType(), csm1.getType());
  }
}
