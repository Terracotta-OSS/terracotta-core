/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.objectserver.gtx.GlobalTransactionDescriptor;

import java.util.Collection;

public interface TransactionPersistor {
  
  public Collection loadAllGlobalTransactionDescriptors();
    
  public void saveGlobalTransactionDescriptor(PersistenceTransaction tx, GlobalTransactionDescriptor gtx);

  public void deleteAllByServerTransactionID(PersistenceTransaction tx, Collection toDelete);
}
