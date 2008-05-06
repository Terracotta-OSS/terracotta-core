/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.sequence;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

public class SequenceGenerator {

  public static class SequenceGeneratorException extends Exception {

    public SequenceGeneratorException(Exception e) {
      super(e);
    }

  }

  public interface SequenceGeneratorListener {

    public void sequenceCreatedFor(Object key) throws SequenceGeneratorException;

    public void sequenceDestroyedFor(Object key);

  }

  private final ConcurrentReaderHashMap   map = new ConcurrentReaderHashMap();
  private final SequenceGeneratorListener listener;

  public SequenceGenerator() {
    this(null);
  }

  public SequenceGenerator(SequenceGeneratorListener listener) {
    this.listener = listener;
  }

  public long getNextSequence(Object key) throws SequenceGeneratorException {
    Sequence seq = (Sequence) map.get(key);
    if (seq != null) return seq.next();
    synchronized (map) {
      if (!map.containsKey(key)) {
        if (listener != null) listener.sequenceCreatedFor(key);
        map.put(key, (seq = new SimpleSequence()));
      } else {
        seq = (Sequence) map.get(key);
      }
    }
    return seq.next();
  }

  public void clearSequenceFor(Object key) {
    if (map.remove(key) != null && listener != null) {
      listener.sequenceDestroyedFor(key);
    }
  }

}
