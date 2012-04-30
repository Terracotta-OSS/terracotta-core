/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.ValueID;

import java.util.List;

/**
 * Context holding search index creation information.
 * 
 * @author Nabib El-Rahman
 */
public class SearchUpsertContext extends BaseSearchEventContext {

  private final List<NVPair> attributes;
  private final String       cacheKey;
  private final ValueID      cacheValue;
  private final boolean      isInsert;

  public SearchUpsertContext(ObjectID segmentOid, String name, String cacheKey, ValueID cacheValue,
                             List<NVPair> attributes, MetaDataProcessingContext metaDataContext, final boolean isInsert) {
    super(segmentOid, name, metaDataContext);
    this.cacheKey = cacheKey;
    this.cacheValue = cacheValue;
    this.attributes = attributes;
    this.isInsert = isInsert;
  }

  /**
   * Key for cache entry.
   */
  public String getCacheKey() {
    return cacheKey;
  }

  /**
   * Value for cache entry
   */
  public ValueID getCacheValue() {
    return cacheValue;
  }

  /**
   * Return List of attributes-value associated with the key.
   */
  public List<NVPair> getAttributes() {
    return attributes;
  }

  public boolean isInsert() {
    return isInsert;
  }

}
