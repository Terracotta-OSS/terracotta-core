/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.config.cache;

import org.junit.Test;
import org.terracotta.toolkit.builder.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.config.Configuration;

import junit.framework.TestCase;

public class InternalCacheConfigurationTypeTest extends TestCase {

  @Test
  public void testMaxBytesLocalOffHeap() {
    Configuration configuration = new ToolkitStoreConfigBuilder().maxBytesLocalOffheap(Long.MAX_VALUE).build();
    InternalCacheConfigurationType.MAX_BYTES_LOCAL_OFFHEAP.validateLegalValue(configuration
        .getObjectOrNull(InternalCacheConfigurationType.MAX_BYTES_LOCAL_OFFHEAP.getConfigString()));
  }

  @Test
  public void testMaxBytesLocalOnHeap() {
    Configuration configuration = new ToolkitStoreConfigBuilder().maxBytesLocalHeap(Long.MAX_VALUE).build();
    InternalCacheConfigurationType.MAX_BYTES_LOCAL_HEAP.validateLegalValue(configuration
        .getObjectOrNull(InternalCacheConfigurationType.MAX_BYTES_LOCAL_HEAP.getConfigString()));

  }

}
