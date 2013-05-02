package com.tc.objectserver.persistence;

import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.EvictableEntry;

import java.util.Map;
import java.util.Set;


public interface EvictionTransactionPersistor {

  public void saveEviction(ServerTransactionID serverTransactionID, final ObjectID oid, final String cacheName, final Map<Object, EvictableEntry> samples);

  public EvictionRemoveContext getEviction(ServerTransactionID serverTransactionID);

  public void removeEviction(ServerTransactionID serverTransactionID);

  public Set<ServerTransactionID> getPersistedTransactions();

}
