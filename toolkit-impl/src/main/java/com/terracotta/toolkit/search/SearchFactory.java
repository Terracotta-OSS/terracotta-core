/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.search;


import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.search.QueryBuilder;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.ValuesResolver;

public interface SearchFactory {

  <K, V> SearchExecutor createSearchExecutor(String name, ToolkitObjectType objectType,
                                             ValuesResolver<K, V> valuesResolver, boolean eventual,
                                             PlatformService platformService);

  QueryBuilder createQueryBuilder(SearchableEntity searchable, ToolkitObjectType objectType);

}
