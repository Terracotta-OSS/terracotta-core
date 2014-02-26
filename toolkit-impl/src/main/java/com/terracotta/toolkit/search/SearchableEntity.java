/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.search;

import org.terracotta.toolkit.search.SearchQueryResultSet;
import org.terracotta.toolkit.search.ToolkitSearchQuery;

import com.tc.search.SearchRequestID;

public interface SearchableEntity {

  /**
   * Run specified query against this searchable object
   */
  SearchQueryResultSet executeQuery(ToolkitSearchQuery query);

  /**
   * Identifies this entity
   */
  String getName();

  /**
   * Closes result set on the server
   */
  void closeResultSet(SearchRequestID reqId);
}
