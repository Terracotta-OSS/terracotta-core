/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.tx.ServerTransactionID;

/**
 * Context holding search index clear information.
 * 
 * @author teck
 */
public class SearchClearContext extends BaseSearchEventContext {

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
}