package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.TransformerLookup;

import java.io.IOException;
import java.util.Map;

/**
 * @author tim
 */
public interface StorageManagerFactory {
  StorageManager createStorageManager(Map<String, KeyValueStorageConfig<?, ?>> configMap, TransformerLookup transformerLookup) throws IOException;
  StorageManager createMetadataStorageManager(Map<String, KeyValueStorageConfig<?, ?>> configMap) throws IOException;
}
