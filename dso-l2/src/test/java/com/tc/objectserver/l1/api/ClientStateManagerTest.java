/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.invalidation.Invalidations;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.util.ObjectIDSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
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
                                                                lookupObjectIDs, new Invalidations()).size() == 0);
    assertEquals(0, lookupObjectIDs.size());

    stateManager.startupNode(cid0);
    stateManager.addReference(cid0, new ObjectID(4));
    stateManager.addAllReferencedIdsTo(testSet);
    assertEquals(0, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new ApplyTransactionInfo(), cid1,
                                                                     lookupObjectIDs, new Invalidations()).size());
    assertEquals(0, lookupObjectIDs.size());
    assertEquals(1, testSet.size());

    testSet = new HashSet();
    stateManager.addReference(cid0, new ObjectID(1));
    stateManager.addAllReferencedIdsTo(testSet);
    assertEquals(2, testSet.size());

    assertEquals(0, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new ApplyTransactionInfo(), cid1,
                                                                     lookupObjectIDs, new Invalidations()).size());
    assertEquals(0, lookupObjectIDs.size());
    assertEquals(0, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new ApplyTransactionInfo(), cid0,
                                                                     lookupObjectIDs, new Invalidations()).size());
    assertEquals(0, lookupObjectIDs.size());

    stateManager.addReference(cid0, new ObjectID(0));
    stateManager.addAllReferencedIdsTo(testSet);
    assertEquals(3, testSet.size());

    assertEquals(1, stateManager.createPrunedChangesAndAddObjectIDTo(changes, new ApplyTransactionInfo(), cid0,
                                                                     lookupObjectIDs, new Invalidations()).size());
    assertEquals(0, lookupObjectIDs.size());

    ApplyTransactionInfo backReferences = new ApplyTransactionInfo();
    backReferences.addBackReference(new ObjectID(2), new ObjectID(0));
    backReferences.addBackReference(new ObjectID(3), new ObjectID(0));

    assertEquals(1, stateManager.createPrunedChangesAndAddObjectIDTo(changes, backReferences, cid0, lookupObjectIDs,
                                                                     new Invalidations()).size());
    assertEquals(2, lookupObjectIDs.size());

    stateManager.shutdownNode(cid1);

  }

  public void testInvalidations() throws Exception {
    // client 1 has 1 - 100
    // client 2 has 101 - 200
    // client 3 has 151 - 300

    // map id 1 has 1 - 100
    // map id 2 has 101 - 200
    // map id 3 has 201 - 400

    ClientStateManager stateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));
    final ClientID cid1 = new ClientID(1);
    final ClientID cid2 = new ClientID(2);
    final ClientID cid3 = new ClientID(3);
    stateManager.startupNode(cid1);
    stateManager.startupNode(cid2);
    stateManager.startupNode(cid3);

    ObjectID mapid1 = new ObjectID(1001);
    ObjectID mapid2 = new ObjectID(1002);
    ObjectID mapid3 = new ObjectID(1003);

    stateManager.addReference(cid1, mapid1);
    stateManager.addReference(cid1, mapid2);
    stateManager.addReference(cid1, mapid3);
    stateManager.addReference(cid2, mapid1);
    stateManager.addReference(cid2, mapid2);
    stateManager.addReference(cid2, mapid3);
    stateManager.addReference(cid3, mapid1);
    stateManager.addReference(cid3, mapid2);
    stateManager.addReference(cid3, mapid3);

    for (int i = 1; i <= 100; i++) {
      stateManager.addReference(cid1, new ObjectID(i));
    }

    for (int i = 101; i <= 200; i++) {
      stateManager.addReference(cid2, new ObjectID(i));
    }

    for (int i = 151; i <= 300; i++) {
      stateManager.addReference(cid3, new ObjectID(i));
    }

    // Initialization done

    // do invalidations
    ApplyTransactionInfo applyTransactionInfo = new ApplyTransactionInfo();
    for (int i = 1; i <= 50; i++) {
      applyTransactionInfo.invalidate(mapid1, new ObjectID(i));
    }

    for (int i = 101; i <= 175; i++) {
      applyTransactionInfo.invalidate(mapid2, new ObjectID(i));
    }

    for (int i = 201; i <= 400; i++) {
      applyTransactionInfo.invalidate(mapid3, new ObjectID(i));
    }

    Invalidations totalInvalidations = applyTransactionInfo.getObjectIDsToInvalidate();
    ObjectIDSet oidSetInvalidated = totalInvalidations.getObjectIDSetForMapId(mapid1);
    for (int i = 1; i <= 50; i++) {
      oidSetInvalidated.contains(new ObjectID(i));
    }
    oidSetInvalidated = totalInvalidations.getObjectIDSetForMapId(mapid2);
    for (int i = 101; i <= 175; i++) {
      oidSetInvalidated.contains(new ObjectID(i));
    }
    oidSetInvalidated = totalInvalidations.getObjectIDSetForMapId(mapid3);
    for (int i = 201; i <= 400; i++) {
      oidSetInvalidated.contains(new ObjectID(i));
    }

    // Client ID 1 asserts
    Invalidations invalidationsForClient = new Invalidations();
    stateManager.createPrunedChangesAndAddObjectIDTo(Collections.EMPTY_LIST, applyTransactionInfo, cid1,
                                                     new ObjectIDSet(), invalidationsForClient);
    oidSetInvalidated = invalidationsForClient.getObjectIDSetForMapId(mapid1);
    Assert.assertEquals(50, oidSetInvalidated.size());
    for (int i = 1; i <= 50; i++) {
      oidSetInvalidated.contains(new ObjectID(i));
    }

    // Client ID 2 asserts
    invalidationsForClient = new Invalidations();
    stateManager.createPrunedChangesAndAddObjectIDTo(Collections.EMPTY_LIST, applyTransactionInfo, cid2,
                                                     new ObjectIDSet(), invalidationsForClient);
    oidSetInvalidated = invalidationsForClient.getObjectIDSetForMapId(mapid2);
    Assert.assertEquals(75, oidSetInvalidated.size());
    for (int i = 101; i <= 175; i++) {
      oidSetInvalidated.contains(new ObjectID(i));
    }

    // Client ID 3 asserts
    invalidationsForClient = new Invalidations();
    stateManager.createPrunedChangesAndAddObjectIDTo(Collections.EMPTY_LIST, applyTransactionInfo, cid3,
                                                     new ObjectIDSet(), invalidationsForClient);
    oidSetInvalidated = invalidationsForClient.getObjectIDSetForMapId(mapid2);
    Assert.assertEquals(25, oidSetInvalidated.size());
    for (int i = 151; i <= 175; i++) {
      oidSetInvalidated.contains(new ObjectID(i));
    }

    oidSetInvalidated = invalidationsForClient.getObjectIDSetForMapId(mapid3);
    Assert.assertEquals(100, oidSetInvalidated.size());
    for (int i = 201; i <= 300; i++) {
      oidSetInvalidated.contains(new ObjectID(i));
    }
  }
}
