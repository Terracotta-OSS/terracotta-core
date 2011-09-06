/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;

/**
 * Context holding search index deletion information.
 * 
 * @author Nabib El-Rahman
 */
public class SearchDeleteContext extends BaseSearchEventContext {

  private final Object cacheKey;

  public SearchDeleteContext(ObjectID segmentOid, String name, Object cacheKey,
                             MetaDataProcessingContext metaDataContext) {
    super(segmentOid, name, metaDataContext);
    this.cacheKey = cacheKey;
  }

  /**
   * key of cache entry.
   */
  public Object getCacheKey() {
    return cacheKey;
  }

}