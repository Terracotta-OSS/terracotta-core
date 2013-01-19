/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.config;

import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.store.ConfigFieldsInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields;
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;
import org.terracotta.toolkit.store.ToolkitConfigFields.PinningStore;

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
      rv[i] ++;
    }
    return rv;
  }

  public static Configuration getDefaultCacheConfig() {
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
    // populate defaults
    builder.concurrency(ToolkitConfigFields.DEFAULT_CONCURRENCY);
    builder.consistency(Consistency.valueOf(ToolkitConfigFields.DEFAULT_CONSISTENCY));
    builder.localCacheEnabled(ToolkitConfigFields.DEFAULT_LOCAL_CACHE_ENABLED);
    builder.offheapEnabled(ToolkitConfigFields.DEFAULT_OFFHEAP_ENABLED);
    builder.maxBytesLocalHeap(ToolkitConfigFields.DEFAULT_MAX_BYTES_LOCAL_HEAP);
    builder.maxBytesLocalOffheap(ToolkitConfigFields.DEFAULT_MAX_BYTES_LOCAL_OFFHEAP);
    builder.maxCountLocalHeap(ToolkitConfigFields.DEFAULT_MAX_COUNT_LOCAL_HEAP);
    builder.compressionEnabled(ToolkitConfigFields.DEFAULT_COMPRESSION_ENABLED);
    builder.copyOnReadEnabled(ToolkitConfigFields.DEFAULT_COPY_ON_READ_ENABLED);
    builder.maxTotalCount(ToolkitConfigFields.DEFAULT_MAX_TOTAL_COUNT);
    builder.evictionEnabled(ToolkitConfigFields.DEFAULT_EVICTION_ENABLED);
    builder.maxTTISeconds(ToolkitConfigFields.DEFAULT_MAX_TTI_SECONDS);
    builder.maxTTLSeconds(ToolkitConfigFields.DEFAULT_MAX_TTL_SECONDS);
    builder.pinningStore(PinningStore.valueOf(ToolkitConfigFields.DEFAULT_PINNING_STORE));
    builder.configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME,
                        ConfigFieldsInternal.DEFAULT_LOCAL_STORE_MANAGER_NAME);
    return builder.build();
  }

}
