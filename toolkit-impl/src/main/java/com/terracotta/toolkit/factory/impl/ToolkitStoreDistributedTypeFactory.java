/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.builder.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.store.ConfigFieldsInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import com.google.common.base.Preconditions;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.search.SearchFactory;

import java.io.Serializable;
import java.util.Set;


/**
 * @author Eugene Shelestovich
 */
public class ToolkitStoreDistributedTypeFactory<K extends Serializable, V extends Serializable>
    extends BaseDistributedToolkitTypeFactory<K, V> {

  private static final String[] RESTRICTED_FIELDS = { ToolkitConfigFields.EVICTION_ENABLED_FIELD_NAME,
      ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME, ToolkitConfigFields.MAX_TTI_SECONDS_FIELD_NAME,
      ToolkitConfigFields.MAX_TTL_SECONDS_FIELD_NAME };

  public ToolkitStoreDistributedTypeFactory(final SearchFactory searchBuilderFactory,
                                            final ServerMapLocalStoreFactory serverMapLocalStoreFactory) {
    super(searchBuilderFactory, serverMapLocalStoreFactory);
  }

  @Override
  protected Set<InternalCacheConfigurationType> getAllSupportedConfigs() {
    return InternalCacheConfigurationType.getConfigsFor(ToolkitObjectType.STORE);
  }

  @Override
  protected void validateNewConfiguration(final Configuration configuration) {
    // options specific for store
    for (String field : RESTRICTED_FIELDS) {
      Preconditions.checkArgument(!configuration.hasField(field),
          "Store does not support '%s' configuration property", field);
    }
  }

  @Override
  protected Configuration getDefaultConfiguration() {
    final ToolkitStoreConfigBuilder builder = new ToolkitStoreConfigBuilder();
    // populate defaults
    builder.concurrency(ToolkitConfigFields.DEFAULT_CONCURRENCY);
    builder.consistency(ToolkitConfigFields.Consistency.valueOf(ToolkitConfigFields.DEFAULT_CONSISTENCY));
    builder.localCacheEnabled(ToolkitConfigFields.DEFAULT_LOCAL_CACHE_ENABLED);
    builder.offheapEnabled(ToolkitConfigFields.DEFAULT_OFFHEAP_ENABLED);
    builder.maxBytesLocalOffheap(ToolkitConfigFields.DEFAULT_MAX_BYTES_LOCAL_OFFHEAP);
    builder.maxCountLocalHeap(ToolkitConfigFields.DEFAULT_MAX_COUNT_LOCAL_HEAP);
    builder.compressionEnabled(ToolkitConfigFields.DEFAULT_COMPRESSION_ENABLED);
    builder.copyOnReadEnabled(ToolkitConfigFields.DEFAULT_COPY_ON_READ_ENABLED);
    builder.pinnedInLocalMemory(ToolkitConfigFields.DEFAULT_PINNED_IN_LOCAL_MEMORY);
    builder.configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME,
        ConfigFieldsInternal.DEFAULT_LOCAL_STORE_MANAGER_NAME);
    return builder.build();
  }
}
