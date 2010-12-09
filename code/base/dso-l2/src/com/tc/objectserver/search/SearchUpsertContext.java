/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.NodeID;
import com.tc.object.metadata.NVPair;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.metadata.AbstractMetaDataContext;

import java.util.List;

/**
 * Context holding search index creation information.
 * 
 * @author Nabib El-Rahman
 */
public class SearchUpsertContext extends AbstractMetaDataContext implements MultiThreadedEventContext {

  private final String       name;
  private final List<NVPair> attributes;
  private final Object       cacheKey;

  public SearchUpsertContext(NodeID id, TransactionID transactionID, String name, Object cacheKey,
                             List<NVPair> attributes) {
    super(id, transactionID);
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

  /**
   * {@inheritDoc}
   */
  public Object getKey() {
    return getSourceID();
  }

}