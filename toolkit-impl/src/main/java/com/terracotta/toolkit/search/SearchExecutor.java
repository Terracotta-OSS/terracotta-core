/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.search;

import org.terracotta.toolkit.search.SearchException;
import org.terracotta.toolkit.search.SearchQueryResultSet;
import org.terracotta.toolkit.search.ToolkitSearchQuery;

import com.tc.search.SearchRequestID;


/**
 * Objects of this type can be used to execute toolkit search queries
 */
public interface SearchExecutor {

  /**
   * Execute given search query
   * 
   * @param query query to run
   * @param queryId unique id for this query (within this client)
   * @return search query results
   */
  SearchQueryResultSet executeQuery(ToolkitSearchQuery query, SearchRequestID queryId) throws SearchException;

}
