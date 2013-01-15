/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.search;


import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.search.QueryBuilder;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.ValuesResolver;

public class UnsupportedSearchFactory implements SearchFactory {

  public static final SearchFactory INSTANCE = new UnsupportedSearchFactory();

  private UnsupportedSearchFactory() {
    // private
  }

  @Override
  public SearchExecutor createSearchExecutor(String name,  ToolkitObjectType objectType,ValuesResolver valuesResolver, boolean eventual,
                                             PlatformService platformService) {
    throw new UnsupportedOperationException("Search is supported in enterprise version only");
  }

  @Override
  public QueryBuilder createQueryBuilder(SearchableEntity entity, ToolkitObjectType objectType) {
    throw new UnsupportedOperationException("Search is supported in enterprise version only");
  }

}
