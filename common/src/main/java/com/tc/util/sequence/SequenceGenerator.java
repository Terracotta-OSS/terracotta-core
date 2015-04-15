/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  private final Map                       map = new ConcurrentHashMap<Object, Sequence>();
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
