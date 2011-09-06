/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;

/**
 * Context holding search index clear information.
 * 
 * @author teck
 */
public class SearchClearContext extends BaseSearchEventContext {

  public SearchClearContext(ObjectID segmentOid, String name, MetaDataProcessingContext metaDataContext) {
    super(segmentOid, name, metaDataContext);
  }

}