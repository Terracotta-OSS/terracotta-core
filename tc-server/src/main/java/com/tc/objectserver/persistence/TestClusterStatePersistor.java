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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.terracotta.persistence.IPlatformPersistence;

import com.tc.util.Assert;


public class TestClusterStatePersistor extends ClusterStatePersistor {
  public TestClusterStatePersistor(Map<String, Serializable> map) {
    super(new IPlatformPersistence() {
      @Override
      public Serializable loadDataElement(String name) throws IOException {
        return map.get(name);
      }

      @Override
      public Serializable loadDataElementInLoader(String name, ClassLoader loader) throws IOException {
        return map.get(name);
      }

      @Override
      public void storeDataElement(String name, Serializable element) throws IOException {
        map.put(name, element);
      }

      @Override
      public Future<Void> fastStoreSequence(long sequenceIndex, SequenceTuple newEntry, long oldestValidSequenceID) {
        // Not expected in test.
        Assert.fail();
        return null;
      }

      @Override
      public List<SequenceTuple> loadSequence(long sequenceIndex) {
        // Not expected in test.
        Assert.fail();
        return null;
      }

      @Override
      public void deleteSequence(long sequenceIndex) {
        // Not expected in test.
        Assert.fail();
      }
    });
  }

  public TestClusterStatePersistor() {
    this(new HashMap<String, Serializable>());
  }
}
