/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.managedobject.BackReferences;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author steve
 */
public class ClientStateManagerTest extends TestCase {

  public void test() throws Exception {
    Set clients = new HashSet();
    ClientStateManager stateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));

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

    clients.add(new ChannelID(50));
    assertTrue(stateManager.createPrunedChangesAndAddObjectIDTo(changes, new BackReferences(), new ChannelID(50),
                                                                lookupObjectIDs).size() == 0);
    assertEquals(0, lookupObjectIDs.size());

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

    clients.remove(new ChannelID(50));
    stateManager.shutdownClient(new ChannelID(50));

  }
}
