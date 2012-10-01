package com.tc.objectserver.persistence.gb;

import com.tc.gbapi.GBMap;
import com.tc.gbapi.GBMapConfig;
import com.tc.gbapi.impl.GBOnHeapMapConfig;
import com.tc.util.sequence.MutableSequence;

/**
 * @author tim
 */
public class GBSequence implements MutableSequence {

  private final GBMap<String, Long> sequenceMap;
  private final String name;

  GBSequence(GBMap<String, Long> sequenceMap, String name) {
    this.name = name;
    this.sequenceMap = sequenceMap;
    Long current = sequenceMap.get(name);
    if (current == null) {
      current = 0L;
      sequenceMap.put(name, current);
    }
  }

  public static GBMapConfig<String, Long> config() {
    return new GBOnHeapMapConfig<String, Long>(String.class, Long.class);
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
