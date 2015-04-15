/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
