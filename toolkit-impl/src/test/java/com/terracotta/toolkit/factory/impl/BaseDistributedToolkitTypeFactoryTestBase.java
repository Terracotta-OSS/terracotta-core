/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.factory.impl;

import org.junit.Test;
import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.terracotta.toolkit.config.UnclusteredConfiguration;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.object.ToolkitObjectStripe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public abstract class BaseDistributedToolkitTypeFactoryTestBase {
  private static final Random random = new Random(System.currentTimeMillis());

  protected abstract BaseDistributedToolkitTypeFactory createFactory();

  protected void testOverrideForConfig(InternalCacheConfigurationType type, Serializable serverValue,
                                       Serializable userValue, Serializable expectedValue, final String overrideMode) {
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.EHCACHE_CLUSTERED_CONFIG_OVERRIDE_MODE, overrideMode);
    try {
      BaseDistributedToolkitTypeFactory factory = createFactory();

      UnclusteredConfiguration serverConfig = new UnclusteredConfiguration(factory.getDefaultConfiguration());
      type.setValue(serverConfig, serverValue);

      UnclusteredConfiguration userConfig = new UnclusteredConfiguration();
      type.setValue(userConfig, userValue);

      Configuration mergedConfig = factory.newConfigForCreationInLocalNode("test",
          new ToolkitObjectStripe[] { mockStripeWithConfig(serverConfig) }, userConfig);

      assertThat(type.getValueIfExists(mergedConfig), is(expectedValue));
    } finally {
      TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.EHCACHE_CLUSTERED_CONFIG_OVERRIDE_MODE, "NONE");
    }
  }

  protected void testConfigMismatch(InternalCacheConfigurationType type, Serializable serverValue, Serializable userValue,
                                    String overrideMode) {
    TCPropertiesImpl.getProperties()
        .setProperty(TCPropertiesConsts.EHCACHE_CLUSTERED_CONFIG_OVERRIDE_MODE, overrideMode);
    try {
      BaseDistributedToolkitTypeFactory factory = createFactory();

      UnclusteredConfiguration serverConfig = new UnclusteredConfiguration(factory.getDefaultConfiguration());
      type.setValue(serverConfig, serverValue);

      UnclusteredConfiguration userConfig = new UnclusteredConfiguration();
      type.setValue(userConfig, userValue);

      Configuration mergedConfig = factory.newConfigForCreationInLocalNode("test",
          new ToolkitObjectStripe[] { mockStripeWithConfig(serverConfig) }, userConfig);

      factory.validateExistingClusterWideConfigs(new ToolkitObjectStripe[] { mockStripeWithConfig(serverConfig) }, userConfig);

    } finally {
      TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.EHCACHE_CLUSTERED_CONFIG_OVERRIDE_MODE, "NONE");
    }
  }

  private <T extends TCToolkitObject> ToolkitObjectStripe<T> mockStripeWithConfig(Configuration configuration) {
    return when(mock(ToolkitObjectStripe.class).getConfiguration()).thenReturn(configuration).getMock();
  }

  @Test
  public void testOverrideMaxBytesLocalHeap() throws Exception {
    testOverrideForConfig(InternalCacheConfigurationType.MAX_BYTES_LOCAL_HEAP, 100L, 1000L, 1000L, "NONE");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_BYTES_LOCAL_HEAP, 100L, 1000L, 1000L, "GLOBAL");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_BYTES_LOCAL_HEAP, 100L, 1000L, 100L, "ALL");
  }

  @Test
  public void testOverrideMaxBytesLocalOffHeap() throws Exception {
    testOverrideForConfig(InternalCacheConfigurationType.MAX_BYTES_LOCAL_OFFHEAP, 100L, 1000L, 1000L, "NONE");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_BYTES_LOCAL_OFFHEAP, 100L, 1000L, 1000L, "GLOBAL");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_BYTES_LOCAL_OFFHEAP, 100L, 1000L, 100L, "ALL");
  }

  @Test
  public void testOverrideMaxCountLocalHeap() throws Exception {
    testOverrideForConfig(InternalCacheConfigurationType.MAX_COUNT_LOCAL_HEAP, 100, 1000, 1000, "NONE");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_COUNT_LOCAL_HEAP, 100, 1000, 1000, "GLOBAL");
    testOverrideForConfig(InternalCacheConfigurationType.MAX_COUNT_LOCAL_HEAP, 100, 1000, 100, "ALL");
  }

  @Test
  public void testOverrideConcurrencyFails() throws Exception {
    testOverrideForConfig(InternalCacheConfigurationType.CONCURRENCY, 100, 1000, 1000, "NONE");
    testOverrideForConfig(InternalCacheConfigurationType.CONCURRENCY, 100, 1000, 1000, "GLOBAL");
    testOverrideForConfig(InternalCacheConfigurationType.CONCURRENCY, 100, 1000, 1000, "ALL");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMismatchConcurrency() throws Exception {
    testConfigMismatch(InternalCacheConfigurationType.CONCURRENCY, 100, 101, "ALL");
  }

  @Test
  public void testMismatchLocalCacheSettings() throws Exception {
    testConfigMismatch(InternalCacheConfigurationType.MAX_BYTES_LOCAL_HEAP, 100, 1000, "NONE");
    testConfigMismatch(InternalCacheConfigurationType.MAX_COUNT_LOCAL_HEAP, 100, 1000, "NONE");
    testConfigMismatch(InternalCacheConfigurationType.MAX_BYTES_LOCAL_OFFHEAP, 100, 1000, "NONE");
    testConfigMismatch(InternalCacheConfigurationType.OFFHEAP_ENABLED, true, false, "NONE");
    testConfigMismatch(InternalCacheConfigurationType.LOCAL_CACHE_ENABLED, true, false, "NONE");
  }

  @Test
  public void testSingleConcurrencyMultipleStripes() {
   BaseDistributedToolkitTypeFactory stubFactory = createFactory();
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();

    builder.maxTotalCount(100).concurrency(1);
    Configuration[] configs = stubFactory.distributeConfigAmongStripes(builder.build(), 2);
    Assert.assertEquals(2, configs.length);
    Assert.assertEquals(1, configs[0].getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME));
    Assert.assertEquals(0, configs[1].getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME));
  }

  @Test
  public void testMultiConcurrencyMultiStripe() {
    BaseDistributedToolkitTypeFactory stubFactory = createFactory();
    Configuration config = stubFactory.newConfigForCreationInCluster(new ToolkitCacheConfigBuilder().concurrency(4).build());

    Configuration[] configs = stubFactory.distributeConfigAmongStripes(config, 5);
    Assert.assertEquals(5, configs.length);
    for (int i = 0; i < 4; i++) {
      Assert.assertEquals(1, configs[i].getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME));
    }
    Assert.assertEquals(0, configs[4].getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME));

    stubFactory.validateExistingClusterWideConfigs(getToolkitObjectStripes(configs), config);
  }

  private static ToolkitObjectStripe[] getToolkitObjectStripes(final Configuration[] configs) {
    return Lists.transform(Arrays.asList(configs), new Function<Configuration, Object>() {
      @Override
      public Object apply(final Configuration input) {
        return when(mock(ToolkitObjectStripe.class).getConfiguration()).thenReturn(input).getMock();
      }
    }).toArray(new ToolkitObjectStripe[configs.length]);
  }

  @Test
  public void testRandomly() {
    final int testCases = 10000;
    int concurrencyLessTestCases = 0;
    for (int i = 1; i <= testCases; i++) {
      final int randomNumStripes = random.nextInt(1000) + 1; // min value 1
      final int randomConcurrency;
      final boolean concurrencyLessThanStripes = (random.nextInt(2) == 0);
      if (concurrencyLessThanStripes) {
        concurrencyLessTestCases++;
        randomConcurrency = random.nextInt(randomNumStripes) + 1;
      } else {
        randomConcurrency = random.nextInt(5000) + 1;
      }
      doRandomTest(randomNumStripes, randomConcurrency);
      if (i % 1000 == 0) {
        System.out.print(".");
        System.out.flush();
      }
    }
    System.out.println("Done with random " + testCases + " test cases (" + concurrencyLessTestCases
                       + " cases where concurrency<=stripes)");
  }

  private void doRandomTest(int randomNumStripes, int randomConcurrency) {

    final String msg = "numStripes: " + randomNumStripes + ", concurrency: " + randomConcurrency;

    BaseDistributedToolkitTypeFactory stubFactory = createFactory();
    UnclusteredConfiguration configuration = new UnclusteredConfiguration(stubFactory.getDefaultConfiguration());
    InternalCacheConfigurationType.CONCURRENCY.setValue(configuration, randomConcurrency);

    Configuration[] configs = stubFactory.distributeConfigAmongStripes(configuration, randomNumStripes);
    Assert.assertEquals(msg, randomNumStripes, configs.length);

    int overallActualConcurrency = 0;
    Set<Integer> distinctConcurrencyValuesSet = new HashSet<Integer>();
    int largestConcurrency = Integer.MIN_VALUE;
    int leastConcurrency = Integer.MAX_VALUE;
    int largestTotalCount = Integer.MIN_VALUE;
    int leastTotalCount = Integer.MAX_VALUE;
    List<Integer> concurrencies = new ArrayList<Integer>();
    List<Integer> totalCounts = new ArrayList<Integer>();
    for (Configuration config : configs) {
      int concurrency = config.getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME);
      distinctConcurrencyValuesSet.add(concurrency);

      overallActualConcurrency += concurrency;

      concurrencies.add(concurrency);

      largestConcurrency = Math.max(largestConcurrency, concurrency);
      leastConcurrency = Math.min(leastConcurrency, concurrency);
    }

    Assert.assertEquals(msg + " - divided configs' concurrency doesn't sum up to overall concurrency",
                        randomConcurrency, overallActualConcurrency);
    Assert.assertTrue(msg + " - All stripes should get only 2 distinct concurrency values",
                      distinctConcurrencyValuesSet.size() <= 2);
    Assert.assertTrue(msg + " - All stripes should get only 2 distinct maxTotalCount values",
                      distinctConcurrencyValuesSet.size() <= 2);
    Assert.assertTrue(msg + " - not fairly distributed concurrency: largest: " + largestConcurrency + ", least: "
                      + leastConcurrency + " - " + concurrencies, largestConcurrency - leastConcurrency <= 1);
    Assert.assertTrue(msg + " - not fairly distributed maxTotalCount: largest: " + largestTotalCount + ", least: "
                      + leastTotalCount + " - " + totalCounts, largestTotalCount - leastTotalCount <= 1);
  }
}
