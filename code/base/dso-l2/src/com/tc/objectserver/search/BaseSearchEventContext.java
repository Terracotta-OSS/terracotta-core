/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.metadata.AbstractMetaDataContext;

class BaseSearchEventContext extends AbstractMetaDataContext implements SearchEventContext, MultiThreadedEventContext {

  public BaseSearchEventContext(ServerTransactionID txnID) {
    super(txnID);
  }

  public final Object getKey() {
    // XXX: This need to kept consistent with com.tc.objectserver.search.SearchQueryContext.getKey()
    return getSourceID();
  }

}
