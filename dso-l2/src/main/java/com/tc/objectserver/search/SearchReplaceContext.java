/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.ValueID;

import java.util.List;

public class SearchReplaceContext extends BaseSearchEventContext {

  private final List<NVPair> attributes;
  private final String       cacheKey;
  private final ValueID      cacheValue;
  private final ValueID      prevValue;

  public SearchReplaceContext(ObjectID segmentOid, String name, String cacheKey, ValueID cacheValue, ValueID prevValue,
                              List<NVPair> attributes, MetaDataProcessingContext context) {
    super(segmentOid, name, context);
    this.cacheKey = cacheKey;
    this.cacheValue = cacheValue;
    this.prevValue = prevValue;
    this.attributes = attributes;
  }

  public String getCacheKey() {
    return cacheKey;
  }

  public ValueID getCacheValue() {
    return cacheValue;
  }

  public List<NVPair> getAttributes() {
    return attributes;
  }

  public ValueID getPreviousValue() {
    return prevValue;
  }

}
