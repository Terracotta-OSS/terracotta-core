/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.search;


import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.SearchExecutor;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.ValuesResolver;

public interface SearchFactory {

  <K, V> SearchExecutor createSearchExecutor(ValuesResolver<K, V> valuesResolver, boolean eventual,
 PlatformService platformService);

  <K, V> QueryBuilder createQueryBuilder(ToolkitStore<K, V> store);

}
