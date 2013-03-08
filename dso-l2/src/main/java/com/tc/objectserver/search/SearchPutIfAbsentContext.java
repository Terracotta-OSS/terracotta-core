/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.ValueID;

import java.util.List;

public class SearchPutIfAbsentContext extends BaseSearchEventContext {
  private final List<NVPair> attributes;
  private final String       cacheKey;
  private final ValueID      cacheValue;

  public SearchPutIfAbsentContext(ObjectID segmentOid, String name, String cacheKey, ValueID cacheValue,
                                  List<NVPair> attributes, MetaDataProcessingContext metaDataContext) {
    super(segmentOid, name, metaDataContext);
    this.cacheKey = cacheKey;
    this.cacheValue = cacheValue;
    this.attributes = attributes;
  }

  public List<NVPair> getAttributes() {
    return attributes;
  }

  public String getCacheKey() {
    return cacheKey;
  }

  public ValueID getCacheValue() {
    return cacheValue;
  }

}
