/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;

import com.tc.abortable.AbortedOperationException;
import com.tc.platform.PlatformService;
import com.tc.properties.NullTCProperties;
import com.terracotta.toolkit.collections.map.AggregateServerMap;
import com.terracotta.toolkit.config.UnclusteredConfiguration;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.mockl2.test.MockDsoCluster;
import com.terracotta.toolkit.object.ToolkitObjectStripe;

import java.util.concurrent.Callable;

public class AggregateServerMapQuickApiTest {

  private AggregateServerMap       aggregateServerMap;

  private ToolkitObjectStripe[]    stripe;

  @Mock
  private UnclusteredConfiguration config;
  @Mock
  private Callable                 schemaCreator;
  @Mock
  private PlatformService          platformService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    stripe = new ToolkitObjectStripe[0];
    config = new UnclusteredConfiguration();
    config.internalSetConfigMapping(InternalCacheConfigurationType.CONSISTENCY.getConfigString(),
                                    Consistency.STRONG.name());
    when(platformService.getTCProperties()).thenReturn(NullTCProperties.INSTANCE);
    when(platformService.getDsoCluster()).thenReturn(new MockDsoCluster());

    aggregateServerMap = new MockAggregateServerMap(ToolkitObjectType.CACHE, null, null, "TestCache", stripe, config,
                                                    schemaCreator, null, platformService);
  }

  @Test
  public void test_quickSize_never_calls_waitForAllCurrentTransactionsToComplete() throws AbortedOperationException {
    aggregateServerMap.quickSize();
    verify(platformService, never()).waitForAllCurrentTransactionsToComplete();
  }

  @Test
  public void test_quickClear_never_calls_waitForAllCurrentTransactionsToComplete() throws AbortedOperationException {
    aggregateServerMap.quickClear();
    verify(platformService, never()).waitForAllCurrentTransactionsToComplete();
  }

}
