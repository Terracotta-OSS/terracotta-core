/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.io.serializer.api.StringIndex;


public interface Persistor {
  public void close();

  public PersistenceTransactionProvider getPersistenceTransactionProvider();

  public ClientStatePersistor getClientStatePersistor();

  public ManagedObjectPersistor getManagedObjectPersistor();

  public TransactionPersistor getTransactionPersistor();

  public PersistentSequence getGlobalTransactionIDSequence();
  
  public ClassPersistor getClassPersistor();
  
  public StringIndex getStringIndex();
  
  public PersistentCollectionFactory getPersistentCollectionFactory();
}
