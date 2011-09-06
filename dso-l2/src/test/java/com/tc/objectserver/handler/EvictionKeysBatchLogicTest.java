/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class EvictionKeysBatchLogicTest extends TestCase {

  public void testKeysBatchBreakupLogic() {
    for (int size = 1; size <= 500; size++) {
      Set set = new LinkedHashSet();
      for (int i = 0; i < size; i++) {
        set.add(i);
      }
      for (int batchSize = 2; batchSize <= 10; batchSize++) {
        List<Set> evictedKeysInBatches = ServerMapEvictionBroadcastHandler.getEvictedKeysInBatches(set, batchSize);
        int expectedNumBatches = size / batchSize + ((size / batchSize) * batchSize < size ? 1 : 0);
        assertEquals(expectedNumBatches, evictedKeysInBatches.size());

        int whichBatch = 0;
        for (Set batch : evictedKeysInBatches) {
          int start = whichBatch * batchSize;
          for (int i = 0; i < batchSize && i < batch.size(); i++) {
            assertTrue(batch.contains(start + i));
          }
          whichBatch++;
        }
      }
    }

  }
}
