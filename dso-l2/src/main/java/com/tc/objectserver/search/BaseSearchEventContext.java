/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

class BaseSearchEventContext implements SearchEventContext, MultiThreadedEventContext {

  private static final int                INDEX_PER_CACHE     = TCPropertiesImpl
                                                                  .getProperties()
                                                                  .getInt(TCPropertiesConsts.SEARCH_LUCENE_INDEXES_PER_CACHE);
  private static final int                SEDA_SEARCH_THREADS = TCPropertiesImpl.getProperties()
                                                                  .getInt(TCPropertiesConsts.L2_SEDA_SEARCH_THREADS);

  private final MetaDataProcessingContext metaDataContext;
  private final String                    cacheName;
  private final ObjectID                  segmentOid;

  public BaseSearchEventContext(ObjectID segmentOid, String cacheName, MetaDataProcessingContext metaDataContext) {
    this.segmentOid = segmentOid;
    this.cacheName = cacheName;
    this.metaDataContext = metaDataContext;
  }

  public final Object getKey() {
    // Pick the start thread index using cache name
    int threadStart = Math.abs(cacheName.hashCode()) % SEDA_SEARCH_THREADS;
    // Pick the next n threads for n indexes using segment id
    int indexSegment = (int) (Math.abs(segmentOid.toLong()) % INDEX_PER_CACHE);
    return (threadStart + indexSegment) % SEDA_SEARCH_THREADS;
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
