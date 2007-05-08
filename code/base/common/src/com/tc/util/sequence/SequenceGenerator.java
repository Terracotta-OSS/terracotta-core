/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.sequence;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

public class SequenceGenerator {

  ConcurrentReaderHashMap map = new ConcurrentReaderHashMap();

  public long getNextSequence(Object key) {
    Sequence seq = (Sequence) map.get(key);
    if (seq != null) return seq.next();
    synchronized (map) {
      if (!map.containsKey(key)) {
        map.put(key, (seq = new SimpleSequence()));
      } else {
        seq = (Sequence) map.get(key);
      }
    }
    return seq.next();
  }
  
  public void clearSequenceFor(Object key) {
    map.remove(key);
  }

}
