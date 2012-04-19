/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.metadata;

import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;

/**
 * Manager to process Metadata from a DNA
 * 
 * @author Nabib El-Rahman
 */
public interface MetaDataManager {

  /**
   * Process metadata.
   * 
   * @param ServerTransaction transaction associated with metadata reader.
   * @param ApplyTransactionInfo applyinfo associated with the txn.
   * @return boolean if all meta data processing is complete
   */
  public boolean processMetaData(ServerTransaction txn, ApplyTransactionInfo applyInfo);

  public void setTransactionManager(ServerTransactionManager transactionManager);

}
