/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;

/**
 * Context holding search index clear information.
 * 
 * @author teck
 */
public class SearchClearContext extends BaseSearchEventContext {

  public SearchClearContext(ServerTransactionID transactionID, String name, MetaDataProcessingContext metaDataContext) {
    super(transactionID, name, metaDataContext);
  }

}