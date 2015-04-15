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

import java.io.IOException;
import java.util.Map;

/**
 * @author tim
 */
public interface StorageManagerFactory {
  enum Type {LEAF, NODE}
 
  StorageManager createStorageManager(Map<String, KeyValueStorageConfig<?, ?>> configMap, TransformerLookup transformerLookup) throws IOException;

  <K, V> KeyValueStorageConfig<K, V> wrapObjectDBConfig(KeyValueStorageConfig<K, V> baseConfig, Type nt);

  <K, V> KeyValueStorageConfig<K, V> wrapObjectDBConfig(ImmutableKeyValueStorageConfig.Builder<K, V> builder, Type nt);

  <K, V> KeyValueStorageConfig<K, V> wrapMapConfig(KeyValueStorageConfig<K, V> baseConfig);

  <K, V> KeyValueStorageConfig<K, V> wrapMapConfig(ImmutableKeyValueStorageConfig.Builder<K, V> builder);
}
