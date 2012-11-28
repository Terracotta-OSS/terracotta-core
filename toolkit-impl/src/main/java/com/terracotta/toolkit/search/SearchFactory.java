/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.search;


import org.terracotta.toolkit.search.QueryBuilder;

import com.tc.object.bytecode.PlatformService;
import com.terracotta.toolkit.collections.map.ValuesResolver;

public interface SearchFactory {

  <K, V> SearchExecutor createSearchExecutor(ValuesResolver<K, V> valuesResolver, boolean eventual, PlatformService platformService);

  QueryBuilder createQueryBuilder(SearchableEntity searchable);

}
