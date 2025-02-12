/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import org.terracotta.persistence.IPlatformPersistence;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * NOTE: This is loosely a clone of the NullPlatformPersistentStorage class in terracotta-core with some unused
 *  functionality stripped out.
 */
public class PassthroughNullPlatformPersistentStorage implements IPlatformPersistence {
  final Map<String, Serializable> nameToDataMap = new HashMap<String, Serializable>();
  final Map<Long, List<SequenceTuple>> fastSequenceCache = new HashMap<Long, List<SequenceTuple>>();

  @Override
  public synchronized Serializable loadDataElement(String name) throws IOException {
    return nameToDataMap.get(name);
  }

  @Override
  public synchronized Serializable loadDataElementInLoader(String name, ClassLoader loader) throws IOException {
    return nameToDataMap.get(name);
  }

  @Override
  public synchronized void storeDataElement(String name, Serializable element) throws IOException {
    if (null == element) {
      nameToDataMap.remove(name);
    } else {
      nameToDataMap.put(name, element);
    }
  }

  @Override
  public synchronized Future<Void> fastStoreSequence(long sequenceIndex, SequenceTuple newEntry, long oldestValidSequenceID) {
    List<SequenceTuple> oldSequence = fastSequenceCache.get(sequenceIndex);
    List<SequenceTuple> newSequence = new ArrayList<SequenceTuple>();
    if (null != oldSequence) {
      for (SequenceTuple tuple : oldSequence) {
        if (tuple.localSequenceID >= oldestValidSequenceID) {
          newSequence.add(tuple);
        }
      }
      newSequence.add(newEntry);
    }
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
      }
    };
  }

  @Override
  public synchronized List<SequenceTuple> loadSequence(long sequenceIndex) {
    return fastSequenceCache.get(sequenceIndex);
  }

  @Override
  public synchronized void deleteSequence(long sequenceIndex) {
    fastSequenceCache.remove(sequenceIndex);
  }
}
