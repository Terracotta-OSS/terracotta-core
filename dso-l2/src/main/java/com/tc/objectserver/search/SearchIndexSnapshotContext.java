/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.terracottatech.search.QueryID;

public class SearchIndexSnapshotContext extends BaseSearchEventContext {

  private final QueryID snapshotId;
  private final boolean isClose;

  public QueryID getSnapshotId() {
    return snapshotId;
  }

  public boolean isClose() {
    return isClose;
  }

  public SearchIndexSnapshotContext(ObjectID segmentOid, String cacheName, QueryID query, boolean isClose,
                                    MetaDataProcessingContext metaDataContext) {
    super(segmentOid, cacheName, metaDataContext);
    snapshotId = query;
    this.isClose = isClose;
  }

}
