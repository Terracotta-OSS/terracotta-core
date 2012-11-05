package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;

import com.tc.util.UUID;
import com.tc.util.sequence.MutableSequence;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author tim
 */
public class SequenceManager {

  private final ConcurrentMap<String, Sequence> createdSequences =
          new ConcurrentHashMap<String, Sequence>();
  private final KeyValueStorage<String, Long> sequenceMap;
  private final KeyValueStorage<String, String> uuidMap;

  public SequenceManager(KeyValueStorage<String, Long> sequenceMap, final KeyValueStorage<String, String> uuidMap) {
    this.sequenceMap = sequenceMap;
    this.uuidMap = uuidMap;
  }

  public MutableSequence getSequence(String name, long initialValue) {
    Sequence sequence = createdSequences.get(name);
    if (sequence == null) {
      sequence = new Sequence(sequenceMap, uuidMap, name, initialValue);
      Sequence racer = createdSequences.putIfAbsent(name, sequence);
      if (racer != null) {
        sequence = racer;
      }
    }
    return sequence;
  }

  public MutableSequence getSequence(String name) {
    return getSequence(name, 0L);
  }

  public static KeyValueStorageConfig<String, Long> sequenceMapConfig() {
    return new ImmutableKeyValueStorageConfig<String, Long>(String.class, Long.class);
  }

  public static KeyValueStorageConfig<String, String> uuidMapConfig() {
    return new ImmutableKeyValueStorageConfig<String, String>(String.class, String.class);
  }

  private static class Sequence implements MutableSequence {

    private final KeyValueStorage<String, Long> sequenceMap;
    private final String uuid;
    private final String name;

    Sequence(KeyValueStorage<String, Long> sequenceMap, KeyValueStorage<String, String> uuidMap, String name, long initialValue) {
      this.name = name;
      this.sequenceMap = sequenceMap;
      Long current = sequenceMap.get(name);
      if (current == null) {
        uuid = UUID.getUUID().toString();
        current = initialValue;
        sequenceMap.put(name, current);
        uuidMap.put(name, uuid);
      } else {
        uuid = uuidMap.get(name);
        if (uuid == null) {
          throw new IllegalStateException("Sequence '" + name + "' has no uuid!");
        }
      }
    }

    @Override
    public String getUID() {
      return uuid;
    }

    @Override
    public synchronized long nextBatch(long batchSize) {
      Long r = sequenceMap.get(name);
      sequenceMap.put(name, r + batchSize);
      return r;
    }

    @Override
    public synchronized void setNext(long next) {
      if (next < sequenceMap.get(name)) {
        throw new AssertionError("next=" + next + " current=" + sequenceMap.get(name));
      }
      sequenceMap.put(name, next);
    }

    @Override
    public long next() {
      return nextBatch(1);
    }

    @Override
    public synchronized long current() {
      return sequenceMap.get(name);
    }
  }
}
