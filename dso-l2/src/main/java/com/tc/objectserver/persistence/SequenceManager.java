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
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;

import com.tc.util.UUID;
import com.tc.util.sequence.MutableSequence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author tim
 */
public class SequenceManager {
  private static final String SEQUENCE_MAP = "sequence_map";
  private static final String SEQUENCE_UUID_MAP = "sequence_uuid_map";

  private final ConcurrentMap<String, Sequence> createdSequences =
          new ConcurrentHashMap<String, Sequence>();
  private final KeyValueStorage<String, Long> sequenceMap;
  private final KeyValueStorage<String, String> uuidMap;

  public SequenceManager(final StorageManager storageManager) {
    this.sequenceMap = storageManager.getKeyValueStorage(SEQUENCE_MAP, String.class, Long.class);
    this.uuidMap = storageManager.getKeyValueStorage(SEQUENCE_UUID_MAP, String.class, String.class);
  }

  public MutableSequence getSequence(String name, long initialValue) {
    Sequence sequence = createdSequences.get(name);
    if (sequence == null) {
      sequence = new Sequence(sequenceMap, uuidMap, name, initialValue);
      Sequence racer = createdSequences.putIfAbsent(name, sequence);
      if (racer != null) {
        sequence = racer;
      }
    }
    return sequence;
  }

  public MutableSequence getSequence(String name) {
    return getSequence(name, 0L);
  }

  public static void addConfigsTo(final Map<String, KeyValueStorageConfig<?, ?>> configs) {
    configs.put(SEQUENCE_MAP, ImmutableKeyValueStorageConfig.builder(String.class, Long.class).build());
    configs.put(SEQUENCE_UUID_MAP, ImmutableKeyValueStorageConfig.builder(String.class, String.class).build());
  }

  private static class Sequence implements MutableSequence {

    private String uuid;
    private long next;

    private final KeyValueStorage<String, Long> sequenceMap;
    private final KeyValueStorage<String, String> uuidMap;
    private final String name;

    Sequence(KeyValueStorage<String, Long> sequenceMap, KeyValueStorage<String, String> uuidMap, String name, long initialValue) {
      this.name = name;
      this.sequenceMap = sequenceMap;
      this.uuidMap = uuidMap;
      if (sequenceMap.get(name) != null) {
        this.next = sequenceMap.get(name);
      } else {
        this.next = initialValue;
      }
    }

    @Override
    public synchronized String getUID() {
      if (uuid == null) {
        // not cached, try to get it
        uuid = uuidMap.get(name);
        if (uuid == null) {
          // still not there, must not be set.
          uuid = UUID.getUUID().toString();
          uuidMap.put(name, uuid);
        }
      }
      return uuid;
    }

    @Override
    public synchronized long nextBatch(long batchSize) {
      long r = next;
      next += batchSize;
      sequenceMap.put(name, next);
      return r;
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
    public long next() {
      return nextBatch(1);
    }

    @Override
    public synchronized long current() {
      return next;
    }
  }
}
