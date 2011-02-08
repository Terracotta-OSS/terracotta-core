/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.object.metadata.NVPair;
import com.tc.objectserver.metadata.MetaDataProcessingContext;

import java.util.List;

/**
 * Context holding search index creation information.
 * 
 * @author Nabib El-Rahman
 */
public class SearchUpsertContext extends BaseSearchEventContext {

  private final List<NVPair> attributes;
  private final Object       cacheKey;
  private final Object       cacheValue;
  private final boolean      putIfAbsent;

  public SearchUpsertContext(ObjectID segmentOid, String name, Object cacheKey, Object cacheValue,
                             List<NVPair> attributes, MetaDataProcessingContext metaDataContext, boolean putIfAbsent) {
    super(segmentOid, name, metaDataContext);
    this.cacheKey = cacheKey;
    this.cacheValue = cacheValue;
    this.attributes = attributes;
    this.putIfAbsent = putIfAbsent;
  }

  /**
   * Key for cache entry.
   */
  public Object getCacheKey() {
    return cacheKey;
  }

  /**
   * Value for cache entry
   */
  public Object getCacheValue() {
    return cacheValue;
  }

  /**
   * Return List of attributes-value associated with the key.
   */
  public List<NVPair> getAttributes() {
    return attributes;
  }

  public boolean isPutIfAbsent() {
    return putIfAbsent;
  }

}