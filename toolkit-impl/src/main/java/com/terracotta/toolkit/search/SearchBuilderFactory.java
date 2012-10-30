/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.search;

import org.terracotta.toolkit.internal.search.SearchBuilder;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.ValuesResolver;

public interface SearchBuilderFactory {

  <K, V> SearchBuilder createSearchBuilder(ValuesResolver<K, V> valuesResolver, boolean eventual,
                                           PlatformService platformService);

}
