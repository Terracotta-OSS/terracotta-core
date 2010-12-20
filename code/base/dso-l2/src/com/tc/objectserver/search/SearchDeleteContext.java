/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.tx.ServerTransactionID;

/**
 * Context holding search index deletion information.
 * 
 * @author Nabib El-Rahman
 */
public class SearchDeleteContext extends BaseSearchEventContext {

  private final String name;
  private final Object cacheKey;

  public SearchDeleteContext(ServerTransactionID transactionID, String name, Object cacheKey) {
    super(transactionID);
    this.name = name;
    this.cacheKey = cacheKey;
  }

  /**
   * Name of index.
   */
  public String getName() {
    return name;
  }

  /**
   * key of cache entry.
   */
  public Object getCacheKey() {
    return cacheKey;
  }

}