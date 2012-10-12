package com.tc.objectserver.persistence.gb;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author tim
 */
public class GBSequenceManager {

  private final ConcurrentMap<String, GBSequence> createdSequences =
          new ConcurrentHashMap<String, GBSequence>();
  private final KeyValueStorage<String, Long> sequenceMap;

  public GBSequenceManager(KeyValueStorage<String, Long> sequenceMap) {
    this.sequenceMap = sequenceMap;
  }

  public GBSequence getSequence(String name) {
    GBSequence sequence = createdSequences.get(name);
    if (sequence == null) {
      sequence = new GBSequence(sequenceMap, name);
      GBSequence racer = createdSequences.putIfAbsent(name, sequence);
      if (racer != null) {
        sequence = racer;
      }
    }
    return sequence;
  }

  public static KeyValueStorageConfig<String, Long> config() {
    KeyValueStorageConfig<String, Long> config = new KeyValueStorageConfigImpl<String, Long>(String.class, Long.class);
    config.setKeySerializer(StringSerializer.INSTANCE);
    config.setValueSerializer(LongSerializer.INSTANCE);
    return config;
  }
}
