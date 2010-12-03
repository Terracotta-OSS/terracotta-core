/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.object.metadata.NVPair;
import com.tc.object.metadata.ValueType;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.metadata.AbstractMetaDataHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  public void handleMetaDataEvent(EventContext context) throws EventHandlerException {
    if (context instanceof SearchUpsertContext) {
      SearchUpsertContext suc = (SearchUpsertContext) context;

      try {
        Index index = this.indexManager.getIndex(suc.getName());
        if (index == null) {
          this.indexManager.createIndex(suc.getName(), extractSchema(suc.getAttributes()));
          index = this.indexManager.getIndex(suc.getName());
        }
        index.upsert(suc.getCacheKey(), suc.getAttributes());
      } catch (IndexException e) {
        // TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
    } else if (context instanceof SearchDeleteContext) {
      SearchDeleteContext sdc = (SearchDeleteContext) context;
      try {
        Index index = this.indexManager.getIndex(sdc.getName());
        if (index != null) {
          index.remove(sdc.getCacheKey());
        } else {
          // TODO: at least log something here
        }
      } catch (IndexException e) {
        // TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
    } else if (context instanceof SearchQueryContext) {
      SearchQueryContext sqc = (SearchQueryContext) context;

      SearchResult searchResult;
      try {
        searchResult = this.indexManager.searchIndex(sqc.getCacheName(), sqc.getQueryStack(), sqc.includeKeys(),
                                                     sqc.getAttributeSet(), sqc.getSortAttributes(),
                                                     sqc.getAggregators(), sqc.getMaxResults());
        this.searchRequestManager.queryResponse(sqc, searchResult.getQueryResults(),
                                                searchResult.getAggregatorResults());
      } catch (IndexException e) {
        // XXX: log something?
        this.searchRequestManager.queryErrorResponse(sqc, e.getMessage());
      }

    } else {
      throw new AssertionError("Unknown context: " + context);
    }
  }

  private static Map<String, ValueType> extractSchema(List<NVPair> attributes) throws IndexException {
    Map<String, ValueType> schema = new HashMap<String, ValueType>();

    for (NVPair attr : attributes) {
      ValueType prev = schema.put(attr.getName(), attr.getType());
      if (prev != null && attr.getType() != prev) {
        //
        throw new IndexException("Differing types for repeated attribute: " + attr.getName());
      }
    }

    return schema;
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext serverContext = (ServerConfigurationContext) context;
    this.indexManager = serverContext.getIndexManager();
    this.searchRequestManager = serverContext.getSearchRequestManager();
  }

}
