/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.persistence;

import com.tc.util.sequence.MutableSequence;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;


public class SequenceManager {
  private static final String SEQUENCE_MAP = "sequence_map";

  private final ConcurrentMap<String, Sequence> createdSequences =
          new ConcurrentHashMap<>();
  private final KeyValueStorage<String, Long> sequenceMap;

  public SequenceManager(IPersistentStorage storageManager) {
    this.sequenceMap = storageManager.getKeyValueStorage(SEQUENCE_MAP, String.class, Long.class);
  }

  public MutableSequence getSequence(String name) {
    Sequence sequence = createdSequences.get(name);
    if (sequence == null) {
      long initialValue = 0L;
      sequence = new Sequence(sequenceMap, name, initialValue);
      Sequence racer = createdSequences.putIfAbsent(name, sequence);
      if (racer != null) {
        sequence = racer;
      }
    }
    return sequence;
  }

  private static class Sequence implements MutableSequence {
    private long next;

    private final KeyValueStorage<String, Long> sequenceMap;
    private final String name;

    Sequence(KeyValueStorage<String, Long> sequenceMap, String name, long initialValue) {
      this.name = name;
      this.sequenceMap = sequenceMap;
      if (sequenceMap.get(name) != null) {
        this.next = sequenceMap.get(name);
      } else {
        this.next = initialValue;
      }
    }

    @Override
    public synchronized void setNext(long next) {
      if (next < this.next) {
        throw new AssertionError("next=" + next + " current=" + this.next);
      }
      this.next = next;
      sequenceMap.put(name, next);
    }

    @Override
    public synchronized long next() {
      long r = next;
      next += 1;
      sequenceMap.put(name, next);
      return r;
    }

    @Override
    public synchronized long current() {
      return next;
    }
  }
}
