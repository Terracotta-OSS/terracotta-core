package com.tc.objectserver.persistence;

import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;
import org.terracotta.persistence.IPlatformPersistence;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


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
      List<SequenceTuple> oldSequence = fastSequenceCache.get(sequenceIndex);
      List<SequenceTuple> newSequence = new ArrayList<>();
      if (null != oldSequence) {
        for (SequenceTuple tuple : oldSequence) {
          if (tuple.localSequenceID >= oldestValidSequenceID) {
            newSequence.add(tuple);
          }
        }
      }
      newSequence.add(newEntry);
      fastSequenceCache.put(sequenceIndex, newSequence);
      return new Future<Void>() {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
          return false;
        }
        @Override
        public boolean isCancelled() {
          return false;
        }
        @Override
        public boolean isDone() {
          return true;
        }
        @Override
        public Void get() throws InterruptedException, ExecutionException {
          return null;
        }
        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
          return null;
        }};
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
