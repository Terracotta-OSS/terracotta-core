/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.gtx.TransactionCommittedError;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

public class TestTransactionStore implements TransactionStore {

  public final NoExceptionLinkedQueue leastContextQueue             = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue commitContextQueue            = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue loadContextQueue              = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue nextTransactionIDContextQueue = new NoExceptionLinkedQueue();

  private final Map                   volatileMap                   = new HashMap();
  private final SortedSet             ids                           = new TreeSet();
  private final Map                   durableMap                    = new HashMap();
  public long                         idSequence                    = 1;

  public void restart() throws Exception {
    volatileMap.clear();
    ids.clear();
    volatileMap.putAll(durableMap);
    for (Iterator i = volatileMap.values().iterator(); i.hasNext();) {
      GlobalTransactionDescriptor gdesc = (GlobalTransactionDescriptor) i.next();
      GlobalTransactionID gid = gdesc.getGlobalTransactionID();
      ids.add(gid);
    }
  }

  public GlobalTransactionDescriptor getOrCreateTransactionDescriptor(ServerTransactionID stxid) {
    GlobalTransactionDescriptor rv = (GlobalTransactionDescriptor) volatileMap.get(stxid);
    if (rv == null) {
      nextTransactionIDContextQueue.put(stxid);
      rv = new GlobalTransactionDescriptor(stxid, new GlobalTransactionID(idSequence++));
      basicPut(volatileMap, rv);
    }
    return rv;
  }

  private void basicPut(Map map, GlobalTransactionDescriptor txID) {
    map.put(txID.getServerTransactionID(), txID);
  }

  public void commitTransactionDescriptor(PersistenceTransaction transaction, ServerTransactionID stxID) {
    GlobalTransactionDescriptor txID = getTransactionDescriptor(stxID);
    if (txID.isCommitted()) { throw new TransactionCommittedError("Already committed : " + txID); }
    try {
      commitContextQueue.put(new Object[] { transaction, txID });
      if (!volatileMap.containsValue(txID)) throw new AssertionError();
      basicPut(durableMap, txID);
      txID.commitComplete();
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  public GlobalTransactionDescriptor getTransactionDescriptor(ServerTransactionID stxid) {
    try {
      loadContextQueue.put(stxid);
      return (GlobalTransactionDescriptor) volatileMap.get(stxid);
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  public GlobalTransactionID getLeastGlobalTransactionID() {
    leastContextQueue.put(new Object());
    return (GlobalTransactionID) ids.first();
  }

  public void clearCommitedTransactionsBelowLowWaterMark(PersistenceTransaction tx, ServerTransactionID lowWaterMark) {
    for (Iterator iter = volatileMap.entrySet().iterator(); iter.hasNext();) {
      Entry e = (Entry) iter.next();
      ServerTransactionID sid = (ServerTransactionID) e.getKey();
      if (sid.getSourceID().equals(lowWaterMark.getSourceID())) {
        GlobalTransactionDescriptor gdesc = (GlobalTransactionDescriptor) e.getValue();
        if (gdesc.getClientTransactionID().toLong() < lowWaterMark.getClientTransactionID().toLong()) {
          ids.remove(gdesc.getGlobalTransactionID());
          durableMap.remove(sid);
        }
      }
    }
  }

  public void shutdownNode(PersistenceTransaction transaction, NodeID nid) {
    throw new ImplementMe();
  }

  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID) {
    GlobalTransactionDescriptor rv = new GlobalTransactionDescriptor(stxnID, globalTransactionID);
    basicPut(volatileMap, rv);
  }

  public void shutdownAllClientsExcept(PersistenceTransaction tx, Set cids) {
    throw new ImplementMe();
  }

  public void commitAllTransactionDescriptor(PersistenceTransaction persistenceTransaction, Collection stxIDs) {
    for (Iterator i = stxIDs.iterator(); i.hasNext();) {
      ServerTransactionID sid = (ServerTransactionID) i.next();
      commitTransactionDescriptor(persistenceTransaction, sid);
    }
  }

  public void clearCommitedTransactionsBelowLowWaterMark(PersistenceTransaction tx, GlobalTransactionID lowWaterMark) {
    throw new ImplementMe();
  }

}