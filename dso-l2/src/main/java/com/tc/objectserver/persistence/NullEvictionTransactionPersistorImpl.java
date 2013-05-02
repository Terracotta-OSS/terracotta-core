package com.tc.objectserver.persistence;


import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.EvictableEntry;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class NullEvictionTransactionPersistorImpl implements EvictionTransactionPersistor {

  @Override
  public EvictionRemoveContext getEviction(ServerTransactionID serverTransactionID) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void saveEviction(ServerTransactionID serverTransactionID, final ObjectID oid, final String cacheName, final Map<Object, EvictableEntry> samples) {

  }

  @Override
  public void removeEviction(ServerTransactionID serverTransactionID) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<ServerTransactionID> getPersistedTransactions() {
    return Collections.emptySet();
  }
}
