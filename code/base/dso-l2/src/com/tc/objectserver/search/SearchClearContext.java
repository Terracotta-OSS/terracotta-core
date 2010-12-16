/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.metadata.AbstractMetaDataContext;

/**
 * Context holding search index clear information.
 * 
 * @author teck
 */
public class SearchClearContext extends AbstractMetaDataContext implements MultiThreadedEventContext {

  private final String name;

  public SearchClearContext(ServerTransactionID transactionID, String name) {
    super(transactionID);
    this.name = name;
  }

  /**
   * Name of index.
   */
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  public Object getKey() {
    return getSourceID();
  }

}