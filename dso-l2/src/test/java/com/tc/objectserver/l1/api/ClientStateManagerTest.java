/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;

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

    ClientID cid0 = new ClientID(0);
    ClientID cid1 = new ClientID(1);
    stateManager.startupNode(cid1);
    assertTrue(stateManager.createPrunedChangesAndAddObjectIDTo(changes, new ApplyTransactionInfo(), cid1,
                                                                lookupObjectIDs, new HashSet()).size() == 0);
    assertEquals(0, lookupObjectIDs.size());

    stateManager.startupNode(cid0);
    stateManager.addReference(cid0, new ObjectID(4));
    stateManager.addAllReferencedIdsTo(testSet);
    assertEquals(0, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new ApplyTransactionInfo(), cid1,
                                                                     lookupObjectIDs, new HashSet()).size());
    assertEquals(0, lookupObjectIDs.size());
    assertEquals(1, testSet.size());

    testSet = new HashSet();
    stateManager.addReference(cid0, new ObjectID(1));
    stateManager.addAllReferencedIdsTo(testSet);
    assertEquals(2, testSet.size());

    assertEquals(0, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new ApplyTransactionInfo(), cid1,
                                                                     lookupObjectIDs, new HashSet()).size());
    assertEquals(0, lookupObjectIDs.size());
    assertEquals(0, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new ApplyTransactionInfo(), cid0,
                                                                     lookupObjectIDs, new HashSet()).size());
    assertEquals(0, lookupObjectIDs.size());

    stateManager.addReference(cid0, new ObjectID(0));
    stateManager.addAllReferencedIdsTo(testSet);
    assertEquals(3, testSet.size());

    assertEquals(1, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new ApplyTransactionInfo(), cid0,
                                                                     lookupObjectIDs, new HashSet()).size());
    assertEquals(0, lookupObjectIDs.size());

    ApplyTransactionInfo backReferences = new ApplyTransactionInfo();
    backReferences.addBackReference(new ObjectID(2), new ObjectID(0));
    backReferences.addBackReference(new ObjectID(3), new ObjectID(0));

    assertEquals(1, stateManager.createPrunedChangesAndAddObjectIDTo(changes, backReferences, cid0, lookupObjectIDs,
                                                                     new HashSet()).size());
    assertEquals(2, lookupObjectIDs.size());

    stateManager.shutdownNode(cid1);

  }
}
