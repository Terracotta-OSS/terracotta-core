/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.inmemory;

import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.storage.api.PersistenceTransaction;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;

public class NullTransactionPersistor implements TransactionPersistor {

  public Collection<GlobalTransactionDescriptor> loadAllGlobalTransactionDescriptors() {
    return Collections.EMPTY_SET;
  }

  public void saveGlobalTransactionDescriptor(PersistenceTransaction tx, GlobalTransactionDescriptor gtx) {
    return;
  }

  public void deleteAllGlobalTransactionDescriptors(PersistenceTransaction tx,
                                                    SortedSet<GlobalTransactionID> globalTransactionIDs) {
    return;
  }
}
