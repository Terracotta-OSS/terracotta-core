/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;

import java.util.Map;

public class SearchEvictionRemoveContext extends BaseSearchEventContext {

  private final Map<Object, Object> toRemove;

  public SearchEvictionRemoveContext(ObjectID segmentOid, String cacheName, Map<Object, Object> toRemove,
                                     MetaDataProcessingContext context) {
    super(segmentOid, cacheName, context);
    this.toRemove = toRemove;
  }

  public Map<Object, Object> getRemoves() {
    return toRemove;
  }

}
