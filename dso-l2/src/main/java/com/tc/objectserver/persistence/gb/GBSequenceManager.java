package com.tc.objectserver.persistence.gb;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.terracotta.corestorage.KeyValueStorage;

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
}
