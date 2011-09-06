/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.metadata;

import com.tc.object.dna.api.MetaDataReader;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;

public class NullMetaDataManager implements MetaDataManager {

  public boolean processMetaDatas(ServerTransaction txn, MetaDataReader[] readers) {
    return true;
  }

  public void setTransactionManager(ServerTransactionManager transactionManager) {
    //
  }

}
