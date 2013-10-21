/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;

import com.tc.abortable.AbortedOperationException;
import com.tc.platform.PlatformService;
import com.tc.properties.NullTCProperties;
import com.terracotta.toolkit.bulkload.BulkLoadToolkitCache;
import com.terracotta.toolkit.collections.map.AggregateServerMap;
import com.terracotta.toolkit.config.UnclusteredConfiguration;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.mockl2.test.MockDsoCluster;
import com.terracotta.toolkit.mockl2.test.ToolkitUnitTest;
import com.terracotta.toolkit.object.ToolkitObjectStripe;

import java.util.concurrent.Callable;

public class BulkLoadToolkitCacheQuickApiTest extends ToolkitUnitTest {
  private BulkLoadToolkitCache     cache;
  private final String             name = "Test-Cache";
  @Mock
  private PlatformService          platformService;
  private AggregateServerMap       aggregateServerMap;
  private ToolkitObjectStripe[]    stripe;
  @Mock
  private Callable                 schemaCreator;
  @Mock
  private UnclusteredConfiguration config;

  @Before
  public void setUp() {
    Toolkit toolkit = getToolKit();
    MockitoAnnotations.initMocks(this);
    when(platformService.getTCProperties()).thenReturn(NullTCProperties.INSTANCE);
    when(platformService.getDsoCluster()).thenReturn(new MockDsoCluster());

    stripe = new ToolkitObjectStripe[0];
    config = new UnclusteredConfiguration();
    config.internalSetConfigMapping(InternalCacheConfigurationType.CONSISTENCY.getConfigString(),
                                    Consistency.STRONG.name());
    aggregateServerMap = new MockAggregateServerMap(ToolkitObjectType.CACHE, null, null, "TestCache", stripe, config,
                                                    schemaCreator, null, platformService);

    cache = new BulkLoadToolkitCache(platformService, name, aggregateServerMap, (ToolkitInternal) toolkit);
  }

  @Test
  public void test_quickSize_never_waits_For_CurrentTransactionsToComplete() throws AbortedOperationException {
    cache.quickSize();
    verify(platformService, never()).waitForAllCurrentTransactionsToComplete();
  }

  @Test
  public void test_quickClear_never_waits_For_CurrentTransactionsToComplete() throws AbortedOperationException {
    cache.quickClear();
    verify(platformService, never()).waitForAllCurrentTransactionsToComplete();
  }

  @After
  public void tearDown() {

  }

}
