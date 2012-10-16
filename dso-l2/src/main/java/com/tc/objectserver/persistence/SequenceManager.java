package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;

import com.tc.util.sequence.MutableSequence;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author tim
 */
public class SequenceManager {

  private final ConcurrentMap<String, GBSequence> createdSequences =
          new ConcurrentHashMap<String, GBSequence>();
  private final KeyValueStorage<String, Long> sequenceMap;

  public SequenceManager(KeyValueStorage<String, Long> sequenceMap) {
    this.sequenceMap = sequenceMap;
  }

  public MutableSequence getSequence(String name) {
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
    return new ImmutableKeyValueStorageConfig<String, Long>(String.class, Long.class);
  }

  private static class GBSequence implements MutableSequence {

    private final KeyValueStorage<String, Long> sequenceMap;
    private final String name;

    GBSequence(KeyValueStorage<String, Long> sequenceMap, String name) {
      this.name = name;
      this.sequenceMap = sequenceMap;
      Long current = sequenceMap.get(name);
      if (current == null) {
        current = 0L;
        sequenceMap.put(name, current);
      }
    }

    @Override
    public String getUID() {
      return name;
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
