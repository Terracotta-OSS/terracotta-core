/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.junit.Test;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.search.SearchFactory;

import static org.mockito.Mockito.mock;

public class ToolkitCacheDistributedTypeFactoryTest extends BaseDistributedToolkitTypeFactoryTestBase {
  @Override
  protected BaseDistributedToolkitTypeFactory createFactory() {
    return new ToolkitCacheDistributedTypeFactory(mock(SearchFactory.class), mock(ServerMapLocalStoreFactory.class));
  }

  @Test
  public void testOverrideMaxTotalCount() throws Exception {
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TOTAL_COUNT, 100, 1000, 1000, "NONE");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TOTAL_COUNT, 100, 1000, 100, "GLOBAL");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TOTAL_COUNT, 100, 1000, 100, "ALL");
  }

  @Test
  public void testOverrideTTI() throws Exception {
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTI_SECONDS, 100, 1000, 1000, "NONE");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTI_SECONDS, 100, 1000, 100, "GLOBAL");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTI_SECONDS, 100, 1000, 100, "ALL");
  }

  @Test
  public void testOverrideTTL() throws Exception {
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTL_SECONDS, 100, 1000, 1000, "NONE");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTL_SECONDS, 100, 1000, 100, "GLOBAL");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_TTL_SECONDS, 100, 1000, 100, "ALL");
  }
}
