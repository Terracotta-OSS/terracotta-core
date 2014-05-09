/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.terracotta.test.categories.CheckShorts;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;

import com.tc.abortable.AbortedOperationException;
import com.tc.platform.PlatformService;
import com.tc.properties.NullTCProperties;
import com.tc.util.concurrent.TaskRunner;
import com.terracotta.toolkit.collections.map.AggregateServerMap;
import com.terracotta.toolkit.collections.map.ServerMap;
import com.terracotta.toolkit.config.UnclusteredConfiguration;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.mockl2.test.MockDsoCluster;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.object.ToolkitObjectStripeImpl;

import java.util.concurrent.Callable;

import junit.framework.Assert;

@Category(CheckShorts.class)
public class AggregateServerMapQuickApiTest {

  private static final int         NUM_OF_ELEMENTS = 50;

  private AggregateServerMap       aggregateServerMap;

  private ToolkitObjectStripe[]    stripe;

  @Mock
  private UnclusteredConfiguration config;
  @Mock
  private Callable                 schemaCreator;
  @Mock
  private PlatformService          platformService;
  @Mock
  private ServerMap                serverMap;
  @Mock
  private TaskRunner               taskRunner;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    config = new UnclusteredConfiguration();
    stripe = new ToolkitObjectStripe[] { new ToolkitObjectStripeImpl(config, new TCToolkitObject[] { serverMap })};
    config.internalSetConfigMapping(InternalCacheConfigurationType.CONSISTENCY.getConfigString(),
                                    Consistency.STRONG.name());
    when(platformService.getTCProperties()).thenReturn(NullTCProperties.INSTANCE);
    when(platformService.getDsoCluster()).thenReturn(new MockDsoCluster());
    when(platformService.getTaskRunner()).thenReturn(taskRunner);

    aggregateServerMap = new MockAggregateServerMap(ToolkitObjectType.CACHE, null, null, "TestCache", stripe, config,
                                                    schemaCreator, null, platformService, mock(ToolkitLock.class));
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

  @Test
  public void test_quickClearWorks() {
    aggregateServerMap.quickClear();
    verify(serverMap).clear();
  }

  @Test
  public void test_quickSize_works() {
    populateMap().assertQuickSize();
  }

  private AggregateServerMapQuickApiTest assertQuickSize() {
    Assert.assertEquals("quickSize failed: quickSize = " + aggregateServerMap.quickSize(), NUM_OF_ELEMENTS,
                        aggregateServerMap.quickSize());
    return this;
  }

  private void assertMapCleared() {
    Assert.assertEquals("size of map = " + aggregateServerMap.size(), 0, aggregateServerMap.size());
  }

  private AggregateServerMapQuickApiTest quickClear() {
    aggregateServerMap.quickClear();
    return this;

  }

  private AggregateServerMapQuickApiTest populateMap() {
    for (int i = 0; i < NUM_OF_ELEMENTS; i++) {
      aggregateServerMap.put("key-" + i, "value-" + i);
    }
    return this;
  }

}
