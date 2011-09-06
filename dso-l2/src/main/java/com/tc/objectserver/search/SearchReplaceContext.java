/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.object.metadata.NVPair;
import com.tc.objectserver.metadata.MetaDataProcessingContext;

import java.util.List;

public class SearchReplaceContext extends BaseSearchEventContext {

  private final List<NVPair> attributes;
  private final Object       cacheKey;
  private final Object       cacheValue;
  private final Object       prevValue;

  public SearchReplaceContext(ObjectID segmentOid, String name, Object cacheKey, Object cacheValue, Object prevValue,
                              List<NVPair> attributes, MetaDataProcessingContext context) {
    super(segmentOid, name, context);
    this.cacheKey = cacheKey;
    this.cacheValue = cacheValue;
    this.prevValue = prevValue;
    this.attributes = attributes;
  }

  public Object getCacheKey() {
    return cacheKey;
  }

  public Object getCacheValue() {
    return cacheValue;
  }

  public List<NVPair> getAttributes() {
    return attributes;
  }

  public Object getPreviousValue() {
    return prevValue;
  }

}