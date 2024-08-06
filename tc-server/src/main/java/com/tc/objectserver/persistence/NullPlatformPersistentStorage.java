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
package com.tc.objectserver.persistence;

import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.persistence.IPlatformPersistence;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;


public class NullPlatformPersistentStorage implements IPlatformPersistence, StateDumpable {
    final Map<String, Serializable> nameToDataMap = new ConcurrentHashMap<>();
    final Map<Long, List<SequenceTuple>> fastSequenceCache = new HashMap<>();

    @Override
    public Serializable loadDataElement(String name) throws IOException {
      return nameToDataMap.get(name);
    }

    @Override
    public Serializable loadDataElementInLoader(String name, ClassLoader loader) throws IOException {
      return nameToDataMap.get(name);
    }

    @Override
    public void storeDataElement(String name, Serializable element) throws IOException {
      if (null == element) {
        nameToDataMap.remove(name);
      } else {
        nameToDataMap.put(name, element);
      }
    }

    @Override
    public synchronized Future<Void> fastStoreSequence(long sequenceIndex, SequenceTuple newEntry, long oldestValidSequenceID) {
      List<SequenceTuple> sequence = fastSequenceCache.get(sequenceIndex);
      if (sequence == null) {
        sequence = new LinkedList<>();
        fastSequenceCache.put(sequenceIndex, sequence);
      }
      if (!sequence.isEmpty()) {
//  exploiting the knowledge that sequences are always updated in an increasing fashion, as soon as the first
//  cleaning function fails, bail on the iteration
        Iterator<SequenceTuple> tuple = sequence.iterator();
        while (tuple.hasNext()) {
          if (tuple.next().localSequenceID < oldestValidSequenceID) {
            tuple.remove();
          } else {
            break;
          }
        }
      }
      sequence.add(newEntry);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized List<SequenceTuple> loadSequence(long sequenceIndex) {
      return fastSequenceCache.get(sequenceIndex);
    }

    @Override
    public synchronized void deleteSequence(long sequenceIndex) {
      fastSequenceCache.remove(sequenceIndex);
    }

    @Override
    public void addStateTo(StateDumpCollector stateDumpCollector) {
        for (Map.Entry<String, Serializable> entry : nameToDataMap.entrySet()) {
          stateDumpCollector.addState("key", entry.getKey());
        }
    }
}
