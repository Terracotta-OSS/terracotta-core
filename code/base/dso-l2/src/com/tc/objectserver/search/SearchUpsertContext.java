/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.NVPair;
import com.tc.object.tx.ServerTransactionID;

import java.util.List;

/**
 * Context holding search index creation information.
 * 
 * @author Nabib El-Rahman
 */
public class SearchUpsertContext extends BaseSearchEventContext {

  private final String       name;
  private final List<NVPair> attributes;
  private final Object       cacheKey;

  public SearchUpsertContext(ServerTransactionID transactionID, String name, Object cacheKey, List<NVPair> attributes) {
    super(transactionID);
    this.name = name;
    this.cacheKey = cacheKey;
    this.attributes = attributes;
  }

  /**
   * Name of index.
   * 
   * @return String name
   */
  public String getName() {
    return name;
  }

  /**
   * Key for cache entry.
   */
  public Object getCacheKey() {
    return cacheKey;
  }

  /**
   * Return List of attributes-value associated with the key.
   */
  public List<NVPair> getAttributes() {
    return attributes;
  }

}