/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util.sequence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

  private final Map<Object, Sequence>     map = new ConcurrentHashMap<Object, Sequence>();
  private final SequenceGeneratorListener listener;

  public SequenceGenerator() {
    this(null);
  }

  public SequenceGenerator(SequenceGeneratorListener listener) {
    this.listener = listener;
  }

  public long getNextSequence(Object key) throws SequenceGeneratorException {
    Sequence seq = map.get(key);
    if (seq != null) return seq.next();
    synchronized (map) {
      if (!map.containsKey(key)) {
        if (listener != null) listener.sequenceCreatedFor(key);
        map.put(key, (seq = new SimpleSequence()));
      } else {
        seq = map.get(key);
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
