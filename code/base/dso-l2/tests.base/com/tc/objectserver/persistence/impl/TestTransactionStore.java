/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
  private final Map                   sids2gid                      = new HashMap();
  public long                         idSequence                    = 1;

  public long nextGlobalTransactionIDBatch(int batchSize) {
    throw new ImplementMe();
  }

  public void restart() throws Exception {
    volatileMap.clear();
    ids.clear();
    sids2gid.clear();
    volatileMap.putAll(durableMap);
    for (Iterator i = volatileMap.values().iterator(); i.hasNext();) {
      ServerTransactionID stxID = ((GlobalTransactionDescriptor) i.next()).getServerTransactionID();
      GlobalTransactionID gid = new GlobalTransactionID(idSequence++);
      ids.add(gid);
      sids2gid.put(stxID, gid);
    }
  }

  public GlobalTransactionDescriptor createTransactionDescriptor(ServerTransactionID stxid) {
    nextTransactionIDContextQueue.put(stxid);
    GlobalTransactionDescriptor rv = new GlobalTransactionDescriptor(stxid);
    basicPut(volatileMap, rv);

    return rv;
  }

  private void basicPut(Map map, GlobalTransactionDescriptor txID) {
    map.put(txID.getServerTransactionID(), txID);
  }

  public void commitTransactionDescriptor(PersistenceTransaction persistenceTransaction, GlobalTransactionDescriptor txID) {
    try {
      commitContextQueue.put(new Object[] { persistenceTransaction, txID });
      if (!volatileMap.containsValue(txID)) throw new AssertionError();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(baos);
      out.writeObject(txID);
      out.flush();
      out.close();
      baos.flush();
      basicPut(durableMap, (GlobalTransactionDescriptor) new ObjectInputStream(new ByteArrayInputStream(baos
          .toByteArray())).readObject());

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
      GlobalTransactionID gid = (GlobalTransactionID) sids2gid.remove(sid);
      if (gid != null) {
        ids.remove(gid);
      }
      volatileMap.remove(sid);
      durableMap.remove(sid);
    }
  }

  public GlobalTransactionID createGlobalTransactionID(ServerTransactionID stxnID) {
    loadContextQueue.put(stxnID);
    GlobalTransactionID gtxID = new GlobalTransactionID(idSequence++);
    ids.add(gtxID);
    sids2gid.put(stxnID, gtxID);
    return gtxID;
  }

  public void shutdownClient(PersistenceTransaction transaction, ChannelID client) {
    throw new ImplementMe();
  }
}