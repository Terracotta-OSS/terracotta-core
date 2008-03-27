/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.TransactionPersistor;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;

public class NullTransactionPersistor implements TransactionPersistor {

  public Collection loadAllGlobalTransactionDescriptors() {
    return Collections.EMPTY_SET;
  }

  public void saveGlobalTransactionDescriptor(PersistenceTransaction tx, GlobalTransactionDescriptor gtx) {
    return;
  }

  public void deleteAllGlobalTransactionDescriptors(PersistenceTransaction tx, SortedSet<ServerTransactionID> toDelete) {
    return;
  }
}
