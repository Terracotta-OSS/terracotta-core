/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.metadata;

import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.tx.ServerTransaction;

public class NullMetaDataManager implements MetaDataManager {

  public boolean processMetaDatas(ServerTransaction txn, MetaDataReader[] readers) {
    return false;
  }

  public boolean metaDataProcessingCompleted(ServerTransactionID id) {
    return false;
  }

}
