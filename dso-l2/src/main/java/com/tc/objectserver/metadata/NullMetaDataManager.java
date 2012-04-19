/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.metadata;

import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;

public class NullMetaDataManager implements MetaDataManager {

  @Override
  public boolean processMetaData(ServerTransaction txn, ApplyTransactionInfo applyInfo) {
    return true;
  }

  public void setTransactionManager(ServerTransactionManager transactionManager) {
    //
  }

}
