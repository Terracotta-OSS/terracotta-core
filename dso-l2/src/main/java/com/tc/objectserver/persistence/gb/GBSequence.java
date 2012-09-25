package com.tc.objectserver.persistence.gb;

import com.tc.objectserver.persistence.gb.gbapi.GBMap;
import com.tc.util.sequence.MutableSequence;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author tim
 */
public class GBSequence implements MutableSequence {

  private final GBMap<String, Long> sequenceMap;
  private final String name;

  public GBSequence(GBMap<String, Long> sequenceMap, String name) {
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
    if (next <= sequenceMap.get(name)) {
      throw new AssertionError();
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
