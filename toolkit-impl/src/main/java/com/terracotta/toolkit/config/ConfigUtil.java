/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.config;

import org.terracotta.toolkit.cache.ToolkitCacheConfigFields;
import org.terracotta.toolkit.cache.ToolkitCacheConfigFields.PinningStore;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.store.ToolkitCacheConfigBuilderInternal;
import org.terracotta.toolkit.internal.store.ToolkitStoreConfigFieldsInternal;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.util.Arrays;

public final class ConfigUtil {
  private static final int[] EMPTY_INT_ARRAY = new int[0];

  private ConfigUtil() {
    // private
  }

  public static int[] distributeInStripes(int configAttrInt, int numStripes) {
    if (numStripes == 0) { return EMPTY_INT_ARRAY; }
    int[] rv = new int[numStripes];
    int least = configAttrInt / numStripes;
    Arrays.fill(rv, least);
    int remainder = configAttrInt % numStripes;
    for (int i = 0; i < remainder; i++) {
      rv[i] = rv[i] + 1;
    }
    return rv;
  }

  public static Configuration getDefaultCacheConfig() {
    ToolkitCacheConfigBuilderInternal builder = new ToolkitCacheConfigBuilderInternal();
    // populate defaults
    builder.concurrency(ToolkitStoreConfigFields.DEFAULT_CONCURRENCY);
    builder.consistency(Consistency.valueOf(ToolkitStoreConfigFields.DEFAULT_CONSISTENCY));
    builder.localCacheEnabled(ToolkitStoreConfigFields.DEFAULT_LOCAL_CACHE_ENABLED);
    builder.offheapEnabled(ToolkitStoreConfigFields.DEFAULT_OFFHEAP_ENABLED);
    builder.maxBytesLocalHeap(ToolkitStoreConfigFields.DEFAULT_MAX_BYTES_LOCAL_HEAP);
    builder.maxBytesLocalOffheap(ToolkitStoreConfigFields.DEFAULT_MAX_BYTES_LOCAL_OFFHEAP);
    builder.maxCountLocalHeap(ToolkitStoreConfigFields.DEFAULT_MAX_COUNT_LOCAL_HEAP);
    builder.compressionEnabled(ToolkitStoreConfigFields.DEFAULT_COMPRESSION_ENABLED);
    builder.copyOnReadEnabled(ToolkitStoreConfigFields.DEFAULT_COPY_ON_READ_ENABLED);
    builder.maxTotalCount(ToolkitCacheConfigFields.DEFAULT_MAX_TOTAL_COUNT);
    builder.maxTTISeconds(ToolkitCacheConfigFields.DEFAULT_MAX_TTI_SECONDS);
    builder.maxTTLSeconds(ToolkitCacheConfigFields.DEFAULT_MAX_TTL_SECONDS);
    builder.pinningStore(PinningStore.valueOf(ToolkitCacheConfigFields.DEFAULT_PINNING_STORE));
    builder.localStoreManagerName(ToolkitStoreConfigFieldsInternal.DEFAULT_LOCAL_STORE_MANAGER_NAME);
    return builder.build();
  }

}
