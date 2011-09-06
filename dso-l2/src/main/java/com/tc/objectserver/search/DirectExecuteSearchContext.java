/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;

abstract class DirectExecuteSearchContext extends BaseSearchEventContext {

  DirectExecuteSearchContext(ObjectID segmentOid, String cacheName, MetaDataProcessingContext metaDataContext) {
    super(segmentOid, cacheName, metaDataContext);
  }

  public abstract void execute() throws IndexException;

}
