/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.search;

import org.terracotta.toolkit.search.SearchQueryResultSet;
import org.terracotta.toolkit.search.ToolkitSearchQuery;

public interface SearchableEntity {

  /**
   * Run specified query against this searchable object
   */
  SearchQueryResultSet executeQuery(ToolkitSearchQuery query);

  /**
   * Identifies this entity
   */
  String getName();
}
