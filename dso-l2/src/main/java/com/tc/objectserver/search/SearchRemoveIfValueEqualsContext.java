/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.terracottatech.search.ValueID;

import java.util.Map;

public class SearchRemoveIfValueEqualsContext extends BaseSearchEventContext {

  private final Map<String, ValueID> toRemove;

  private final boolean              isEviction;

  public SearchRemoveIfValueEqualsContext(ObjectID segmentOid, String cacheName, Map<String, ValueID> toRemove,
                                          MetaDataProcessingContext context, boolean isEviction) {
    super(segmentOid, cacheName, context);
    this.toRemove = toRemove;
    this.isEviction = isEviction;
  }

  public Map<String, ValueID> getRemoves() {
    return toRemove;
  }

  public boolean isEviction() {
    return isEviction;
  }

}
