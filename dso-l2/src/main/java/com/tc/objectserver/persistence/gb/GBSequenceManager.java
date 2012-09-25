package com.tc.objectserver.persistence.gb;

import com.tc.gbapi.GBManager;
import com.tc.gbapi.GBMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author tim
 */
public class GBSequenceManager {

  private final ConcurrentMap<String, GBSequence> createdSequences =
          new ConcurrentHashMap<String, GBSequence>();
  private final GBMap<String, Long> sequenceMap;

  public GBSequenceManager(GBMap<String, Long> sequenceMap) {
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
