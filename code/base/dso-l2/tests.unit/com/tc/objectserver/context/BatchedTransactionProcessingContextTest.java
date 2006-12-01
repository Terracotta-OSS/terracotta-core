/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.objectserver.tx.TestServerTransaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class BatchedTransactionProcessingContextTest extends TestCase {
  
  private Set objectIDs;
  private Map newRoots;
  private List objects;
  private Map idsToObjects;
  private Collection committedGlobalTransactionIDs;
  private BatchedTransactionProcessingContext batchContext;
  private List serverTransactions;
  private Set completedTxnIds;

  public void setUp() {
    objectIDs = new HashSet();
    newRoots = new HashMap();
    objects = new ArrayList();
    idsToObjects = new HashMap();
    committedGlobalTransactionIDs = new HashSet();
    batchContext = new BatchedTransactionProcessingContext();
    serverTransactions = new ArrayList();
    completedTxnIds = new HashSet();
  }
  
  public void tests() throws Exception {
    
    assertTrue(batchContext.isEmpty());
    assertFalse(batchContext.isClosed());
    
    checkValues();
    
    ObjectID objectID = new ObjectID(1);
    Object object = new Object();
    idsToObjects.put(objectID, object);
    ServerTransactionID stxID =  new ServerTransactionID(new ChannelID(1), new TransactionID(1));
    TestServerTransaction stx = new TestServerTransaction(stxID, new TxnBatchID(1));
    
    objectIDs.add(objectID);
    objects.add(object);
    serverTransactions.add(stx);
    
    completedTxnIds.add(new ServerTransactionID(new ChannelID(1), new TransactionID(0)));
    
    batchContext.addTransaction(stx);
    batchContext.addLookedUpObjects(objectIDs, idsToObjects);

    batchContext.addAppliedServerTransactionIDsTo(stxID);
    committedGlobalTransactionIDs.add(stxID);
    checkValues();

    batchContext.close(completedTxnIds);
    assertEquals(serverTransactions, iteratorAsList(batchContext.iterator()));
  }

  private void checkValues() {
    assertEquals(newRoots, batchContext.getNewRoots());
    assertEquals(objectIDs, batchContext.getObjectIDs());
    assertEquals(objects, new ArrayList(batchContext.getObjects()));
    assertEquals(committedGlobalTransactionIDs, batchContext.getAppliedServerTransactionIDs());
    assertEquals(serverTransactions, batchContext.getTxns());
  }

  private List iteratorAsList(Iterator i) {
    List rv = new LinkedList();
    for (;i.hasNext();) {
      rv.add(i.next());
    }
    return rv;
  }
}