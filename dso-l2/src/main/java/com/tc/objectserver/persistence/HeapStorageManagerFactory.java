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
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.TransformerLookup;
import org.terracotta.corestorage.heap.HeapStorageManager;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.Map;

/**
 * @author tim
 */
public class HeapStorageManagerFactory implements StorageManagerFactory {
  private static final TCLogger logger = TCLogging.getLogger(HeapStorageManagerFactory.class);

  public static final HeapStorageManagerFactory INSTANCE = new HeapStorageManagerFactory();

  private HeapStorageManagerFactory() {
    // Use the singleton instance
  }

  @Override
  public StorageManager createStorageManager(final Map<String, KeyValueStorageConfig<?, ?>> configMap,
                                             final TransformerLookup transformerLookup) {
    logger.warn("Using heap L2 storage is not recommended. It can lead to inconsistent eviction behavior or OOMEs.");
    return new HeapStorageManager(configMap);
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapObjectDBConfig(final KeyValueStorageConfig<K, V> baseConfig, Type nt) {
    return baseConfig;
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapMapConfig(final KeyValueStorageConfig<K, V> baseConfig) {
    return baseConfig;
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapObjectDBConfig(final ImmutableKeyValueStorageConfig.Builder<K, V> builder, Type nt) {
    return builder.build();
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapMapConfig(final ImmutableKeyValueStorageConfig.Builder<K, V> builder) {
    return builder.build();
  }
}
