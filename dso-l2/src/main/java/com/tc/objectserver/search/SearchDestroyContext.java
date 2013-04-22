package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;

/**
 * @author tim
 */
public class SearchDestroyContext extends BaseSearchEventContext {
  public SearchDestroyContext(final ObjectID segmentOid, final String cacheName, final MetaDataProcessingContext metaDataContext) {
    super(segmentOid, cacheName, metaDataContext);
  }
}
