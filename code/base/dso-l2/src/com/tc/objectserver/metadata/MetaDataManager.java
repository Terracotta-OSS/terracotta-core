/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.metadata;

import com.tc.object.dna.api.MetaDataReader;
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
   * @param MetaDataReader metadata reader associated with a DNA.
   * @return boolean if all meta data processing is complete
   */
  public boolean processMetaDatas(ServerTransaction txn, MetaDataReader[] readers);

  public void setTransactionManager(ServerTransactionManager transactionManager);

}
