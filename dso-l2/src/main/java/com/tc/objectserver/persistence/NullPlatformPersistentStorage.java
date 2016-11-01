package com.tc.objectserver.persistence;

import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;
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
    public void dumpStateTo(StateDumper stateDumper) {
        for (Map.Entry<String, Serializable> entry : nameToDataMap.entrySet()) {
          stateDumper.dumpState("key", entry.getKey());
        }
    }
}
