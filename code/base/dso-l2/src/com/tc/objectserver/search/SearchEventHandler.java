/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;

import java.io.IOException;

/**
 * All search request are processed through this handler. Every context should implement
 * {@link MultiThreadedEventContext} so that order can be maintained per client.
 * 
 * @author Nabib El-Rahman
 */
public class SearchEventHandler extends AbstractEventHandler {

  private IndexManager         indexManager;
  private SearchRequestManager searchRequestManager;

  /**
   * {@inheritDoc}
   * 
   * @throws IOException
   */
  @Override
  public void handleEvent(EventContext context) throws EventHandlerException {
    if (context instanceof SearchUpsertContext) {
      SearchUpsertContext suc = (SearchUpsertContext) context;

      try {
        this.indexManager.upsert(suc.getCacheName(), suc.getCacheKey(), suc.getCacheValue(), suc.getAttributes(),
                                 suc.getMetaDataProcessingContext());
      } catch (IndexException e) {
        // TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
    } else if (context instanceof SearchDeleteContext) {
      SearchDeleteContext sdc = (SearchDeleteContext) context;
      try {
        this.indexManager.remove(sdc.getCacheName(), sdc.getCacheKey(), sdc.getMetaDataProcessingContext());
      } catch (IndexException e) {
        // TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
    } else if (context instanceof SearchQueryContext) {
      SearchQueryContext sqc = (SearchQueryContext) context;

      SearchResult searchResult;
      try {
        searchResult = this.indexManager
            .searchIndex(sqc.getCacheName(), sqc.getQueryStack(), sqc.includeKeys(), sqc.includeValues(),
                         sqc.getAttributeSet(), sqc.getSortAttributes(), sqc.getAggregators(), sqc.getMaxResults());
        this.searchRequestManager.queryResponse(sqc, searchResult.getQueryResults(), searchResult.getAggregators(),
                                                sqc.getBatchSize(), sqc.isPrefetchFirstBatch());
      } catch (IndexException e) {
        // XXX: log something?
        this.searchRequestManager.queryErrorResponse(sqc, e.getMessage());
      }
    } else if (context instanceof SearchClearContext) {
      SearchClearContext scc = (SearchClearContext) context;
      try {
        this.indexManager.clear(scc.getCacheName(), scc.getMetaDataProcessingContext());
      } catch (IndexException e) {
        // TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
    } else if (context instanceof SearchEvictionRemoveContext) {
      SearchEvictionRemoveContext serc = (SearchEvictionRemoveContext) context;
      try {
        this.indexManager.removeIfValueEqual(serc.getCacheName(), serc.getRemoves(),
                                             serc.getMetaDataProcessingContext());
      } catch (IndexException e) {
        // TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
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
