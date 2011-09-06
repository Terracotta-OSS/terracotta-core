/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;

class BaseSearchEventContext implements SearchEventContext, MultiThreadedEventContext {

  private final MetaDataProcessingContext metaDataContext;
  private final String                    cacheName;
  private final ObjectID                  segmentOid;

  public BaseSearchEventContext(ObjectID segmentOid, String cacheName, MetaDataProcessingContext metaDataContext) {
    this.segmentOid = segmentOid;
    this.cacheName = cacheName;
    this.metaDataContext = metaDataContext;
  }

  public final Object getKey() {
    return segmentOid;
  }

  public MetaDataProcessingContext getMetaDataProcessingContext() {
    return metaDataContext;
  }

  public String getCacheName() {
    return cacheName;
  }

  public ObjectID getSegmentOid() {
    return segmentOid;
  }
}
