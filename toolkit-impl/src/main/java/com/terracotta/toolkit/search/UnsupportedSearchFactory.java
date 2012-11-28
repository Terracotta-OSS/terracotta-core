/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.search;


import org.terracotta.toolkit.search.QueryBuilder;

import com.tc.object.bytecode.PlatformService;
import com.terracotta.toolkit.collections.map.ValuesResolver;

public class UnsupportedSearchFactory implements SearchFactory {

  public static final SearchFactory INSTANCE = new UnsupportedSearchFactory();

  private UnsupportedSearchFactory() {
    // private
  }

  @Override
  public SearchExecutor createSearchExecutor(ValuesResolver valuesResolver, boolean eventual, PlatformService platformService) {
    throw new UnsupportedOperationException("Search is supported in enterprise version only");
  }

  @Override
  public QueryBuilder createQueryBuilder(SearchableEntity entity) {
    throw new UnsupportedOperationException("Search is supported in enterprise version only");
  }

}
