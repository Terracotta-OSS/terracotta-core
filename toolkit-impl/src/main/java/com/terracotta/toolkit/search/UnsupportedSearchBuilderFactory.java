/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.search;

import org.terracotta.toolkit.internal.search.SearchBuilder;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.ValuesResolver;

public class UnsupportedSearchBuilderFactory implements SearchBuilderFactory {

  public static final SearchBuilderFactory INSTANCE = new UnsupportedSearchBuilderFactory();

  private UnsupportedSearchBuilderFactory() {
    // private
  }

  @Override
  public SearchBuilder createSearchBuilder(ValuesResolver valuesResolver, boolean eventual,
                                           PlatformService platformService) {
    throw new UnsupportedOperationException("Search is supported in enterprise version only");
  }

}
