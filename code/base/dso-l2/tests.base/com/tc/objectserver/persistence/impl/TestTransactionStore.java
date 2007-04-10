/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.gtx.TransactionCommittedError;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestTransactionStore implements TransactionStore {

  public final NoExceptionLinkedQueue leastContextQueue             = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue commitContextQueue            = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue loadContextQueue              = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue nextTransactionIDContextQueue = new NoExceptionLinkedQueue();

  private final Map                   volatileMap                   = new HashMap();
  private final SortedSet             ids                           = new TreeSet(GlobalTransactionID.COMPARATOR);
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
    nextTransactionIDContextQueue.put(stxid);
    GlobalTransactionDescriptor rv = new GlobalTransactionDescriptor(stxid, new GlobalTransactionID(idSequence++));
    basicPut(volatileMap, rv);
    return rv;
  }

  private void basicPut(Map map, GlobalTransactionDescriptor txID) {
    map.put(txID.getServerTransactionID(), txID);
  }

  public void commitTransactionDescriptor(PersistenceTransaction persistenceTransaction,
                                          GlobalTransactionDescriptor txID) {
    if (txID.isCommitted()) { throw new TransactionCommittedError("Already committed : " + txID); }
    try {
      commitContextQueue.put(new Object[] { persistenceTransaction, txID });
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

  public void removeAllByServerTransactionID(PersistenceTransaction tx, Collection collection) {
    for (Iterator iter = collection.iterator(); iter.hasNext();) {
      ServerTransactionID sid = (ServerTransactionID) iter.next();
      GlobalTransactionDescriptor gdesc = (GlobalTransactionDescriptor) volatileMap.remove(sid);
      if (gdesc != null) {
        ids.remove(gdesc.getGlobalTransactionID());
      }
      durableMap.remove(sid);
    }
  }

  public GlobalTransactionID getGlobalTransactionID(ServerTransactionID stxnID) {
    loadContextQueue.put(stxnID);
    GlobalTransactionDescriptor gdesc = (GlobalTransactionDescriptor) volatileMap.get(stxnID);
    if (gdesc == null) {
      return GlobalTransactionID.NULL_ID;
    } else {
      return gdesc.getGlobalTransactionID();
    }
  }

  public void shutdownClient(PersistenceTransaction transaction, ChannelID client) {
    throw new ImplementMe();
  }

  public void createGlobalTransactionDesc(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID) {
    GlobalTransactionDescriptor rv = new GlobalTransactionDescriptor(stxnID, globalTransactionID);
    basicPut(volatileMap, rv);
  }
}