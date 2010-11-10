/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.metadata.AbstractMetaDataHandler;

import java.io.IOException;

/**
 * All search request are processed through this handler. Every context should implement
 * {@link MultiThreadedEventContext} so that order can be maintained per client.
 * 
 * @author Nabib El-Rahman
 */
public class SearchEventHandler extends AbstractMetaDataHandler {

  private IndexManager         indexManager;
  private SearchRequestManager searchRequestManager;

  /**
   * {@inheritDoc}
   * 
   * @throws IOException
   */
  @Override
  public void handleMetaDataEvent(EventContext context) throws EventHandlerException, IOException {
    if (context instanceof SearchUpsertContext) {
      SearchUpsertContext sicc = (SearchUpsertContext) context;

      try {
        Index index = this.indexManager.getIndex(sicc.getName());
        if (index == null) {
          this.indexManager.createIndex(sicc.getName(), sicc.getSchema());
          index = this.indexManager.getIndex(sicc.getName());
        }
        index.upsert(sicc.getCacheKey(), sicc.getAttributes());
      } catch (IndexException e) {
        // TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
    } else if (context instanceof SearchDeleteContext) {
      SearchDeleteContext sidc = (SearchDeleteContext) context;
      try {
        Index index = this.indexManager.getIndex(sidc.getName());
        if (index != null) {
          index.remove(sidc.getCacheKey());
        } else {
          // TODO: at least log something here
        }
      } catch (IndexException e) {
        // TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
    } else if (context instanceof SearchQueryContext) {
      SearchQueryContext sqc = (SearchQueryContext) context;

      IndexContext indexContext = this.indexManager.searchIndex(sqc.getCacheName(), sqc.getQueryStack(), sqc
          .includeKeys(), sqc.getAttributeSet(), sqc.getSortAttributes(), sqc.getAggregators());
      this.searchRequestManager.queryResponse(sqc, indexContext.getQueryResults(), indexContext.getAggregatorResults());
    } else {
      throw new AssertionError("Unknown context: " + context);
    }
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext serverContext = (ServerConfigurationContext) context;
    this.indexManager = serverContext.getIndexManager();
    this.searchRequestManager = serverContext.getSearchRequestManager();
  }

}
