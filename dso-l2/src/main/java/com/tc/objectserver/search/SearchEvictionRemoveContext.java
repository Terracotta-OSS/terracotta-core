/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.terracottatech.search.ValueID;

import java.util.Map;

public class SearchEvictionRemoveContext extends BaseSearchEventContext {

  private final Map<String, ValueID> toRemove;

  public SearchEvictionRemoveContext(ObjectID segmentOid, String cacheName, Map<String, ValueID> toRemove,
                                     MetaDataProcessingContext context) {
    super(segmentOid, cacheName, context);
    this.toRemove = toRemove;
  }

  public Map<String, ValueID> getRemoves() {
    return toRemove;
  }

}
