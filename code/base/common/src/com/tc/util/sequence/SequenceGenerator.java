/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.sequence;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

public class SequenceGenerator {

  public interface SequenceGeneratorListener {
    
    public void sequenceCreatedFor(Object key);
    
    public void sequenceDestroyedFor(Object key);
    
  }

  private final ConcurrentReaderHashMap map = new ConcurrentReaderHashMap();
  private final SequenceGeneratorListener listener;

  public SequenceGenerator() {
    this(null);
  }

  public SequenceGenerator(SequenceGeneratorListener listener) {
    this.listener = listener;
  }

  public long getNextSequence(Object key) {
    Sequence seq = (Sequence) map.get(key);
    if (seq != null) return seq.next();
    synchronized (map) {
      if (!map.containsKey(key)) {
        map.put(key, (seq = new SimpleSequence()));
        if (listener != null) listener.sequenceCreatedFor(key);
      } else {
        seq = (Sequence) map.get(key);
      }
    }
    return seq.next();
  }

  public void clearSequenceFor(Object key) {
    if(map.remove(key) != null && listener != null) {
      listener.sequenceDestroyedFor(key);
    }
  }

}
