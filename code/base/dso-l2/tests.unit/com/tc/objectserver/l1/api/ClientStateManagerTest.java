/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.transport.ConnectionIdFactory;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.persistence.api.ClientStatePersistor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author steve
 */
public class ClientStateManagerTest extends TestCase {

  public void test() throws Exception {
    Set clients = new HashSet();
    TestClientStatePersistor store = new TestClientStatePersistor();
    ClientStateManager stateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class), store);

    Set toGC = new HashSet();
    toGC.add(new ObjectID(0));
    toGC.add(new ObjectID(1));
    toGC.add(new ObjectID(2));
    toGC.add(new ObjectID(3));

    List changes = new LinkedList();
    for (Iterator i = toGC.iterator(); i.hasNext();) {
      ObjectID id = (ObjectID) i.next();
      changes.add(new TestDNA(id, id.toLong() % 2 == 0));
    }
    Set testSet = new HashSet();
    Set lookupObjectIDs = new HashSet();

    assertEquals(0, store.saveContexts.size());
    clients.add(new ChannelID(50));
    assertTrue(stateManager.createPrunedChangesAndAddObjectIDTo(changes, new BackReferences(), new ChannelID(50),
                                                                lookupObjectIDs).size() == 0);
    assertEquals(0, lookupObjectIDs.size());
    assertEquals(1, store.saveContexts.size());
    assertEquals(new ChannelID(50), store.saveContexts.get(0));
    assertEquals(clients, store.clients);
    assertEquals(1, store.clients.size());

    clients.add(new ChannelID(0));
    stateManager.addReference(new ChannelID(0), new ObjectID(4));
    stateManager.addAllReferencedIdsTo(testSet);
    assertEquals(0, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new BackReferences(), new ChannelID(50),
                                                                     lookupObjectIDs).size());
    assertEquals(0, lookupObjectIDs.size());
    assertEquals(1, testSet.size());

    testSet = new HashSet();
    stateManager.addReference(new ChannelID(0), new ObjectID(1));
    stateManager.addAllReferencedIdsTo(testSet);
    assertEquals(2, testSet.size());

    assertEquals(0, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new BackReferences(), new ChannelID(50),
                                                                     lookupObjectIDs).size());
    assertEquals(0, lookupObjectIDs.size());
    assertEquals(0, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new BackReferences(), new ChannelID(0),
                                                                     lookupObjectIDs).size());
    assertEquals(0, lookupObjectIDs.size());

    stateManager.addReference(new ChannelID(0), new ObjectID(0));
    stateManager.addAllReferencedIdsTo(testSet);
    assertEquals(3, testSet.size());

    assertEquals(1, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new BackReferences(), new ChannelID(0),
                                                                     lookupObjectIDs).size());
    assertEquals(0, lookupObjectIDs.size());

    BackReferences backReferences = new BackReferences();
    backReferences.addBackReference(new ObjectID(2), new ObjectID(0));
    backReferences.addBackReference(new ObjectID(3), new ObjectID(0));

    assertEquals(1, stateManager.createPrunedChangesAndAddObjectIDTo(changes, backReferences, new ChannelID(0),
                                                                     lookupObjectIDs).size());
    assertEquals(2, lookupObjectIDs.size());

    assertEquals(0, store.removeContexts.size());
    clients.remove(new ChannelID(50));
    stateManager.shutdownClient(new ChannelID(50));

    assertEquals(1, store.removeContexts.size());
    assertEquals(new ChannelID(50), store.removeContexts.get(0));

    // a new client state manager should initialize itself with existing clients
    Map clientMap = new HashMap();
    new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class), clientMap, store);
    assertEquals(clientMap.keySet(), clients);
  }

  private class TestClientStatePersistor implements ClientStatePersistor {

    public List getClientIDsContexts = new ArrayList();
    public List saveContexts         = new ArrayList();
    public List removeContexts       = new ArrayList();

    public Set  clients              = new HashSet();

    public long nextChangeIDFor(ChannelID id) {
      return -1;
    }

    public boolean containsClient(ChannelID id) {
      return false;
    }

    public Iterator loadClientIDs() {
      getClientIDsContexts.add(new Object());
      return clients.iterator();
    }

    public void saveClientState(ClientState clientState) {
      saveContexts.add(clientState.getClientID());
      clients.add(clientState.getClientID());
    }

    public void deleteClientState(ChannelID id) {
      removeContexts.add(id);
      clients.remove(id);
    }

    public ConnectionIdFactory getConnectionIDFactory() {
      // TODO Auto-generated method stub
      return null;
    }

    public Set loadConnectionIDs() {
      throw new ImplementMe();
    }

  }

}
