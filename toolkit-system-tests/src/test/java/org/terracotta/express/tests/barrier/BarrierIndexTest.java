/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.barrier;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.BrokenBarrierException;

/**
 * Checks whether the consistency of index numbers returned by {@link ToolkitBarrier#await()}
 */
public class BarrierIndexTest extends AbstractToolkitTestBase {

  private final static int NUM_CLIENTS = 8;

  public BarrierIndexTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(BarrierClient.class, NUM_CLIENTS);
    testConfig.getClientConfig().setParallelClients(true);
  }


  public static class BarrierClient extends ClientBase {

    private final static int NUM_ITERATIONS = 100;

    public BarrierClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      testAcyclic(toolkit);
      testCyclic(toolkit);
    }


    private void testAcyclic(Toolkit toolkit) throws InterruptedException, BrokenBarrierException {
      for (int i = 0; i < NUM_ITERATIONS; i++) {
        ToolkitBarrier syncBarrier = toolkit.getBarrier("sync_barrier", getParticipantCount());
        ToolkitBarrier barrier = toolkit.getBarrier("simple_barrier", getParticipantCount());
        int index = barrier.await();
        ToolkitSortedSet sortedSet = toolkit.getSortedSet("sorted_index_set", Integer.class);
        assertTrue(sortedSet.add(index));
        syncBarrier.await();// Check conditions when all parties have arrived.
        if (index == 0) {
          assertEquals(getParticipantCount(), sortedSet.size());
          assertEquals(0, sortedSet.first());
          assertEquals(getParticipantCount() - 1, sortedSet.last());
          sortedSet.destroy();
        }
      }
    }

    /**
     * Tests the {@link ToolkitBarrier} as a CyclicBarrier
     */
    private void testCyclic(Toolkit toolkit) throws InterruptedException, BrokenBarrierException {
      ToolkitBarrier syncBarrier = toolkit.getBarrier("sync_barrier", getParticipantCount());
      ToolkitBarrier barrier = toolkit.getBarrier("simple_barrier", getParticipantCount());

      for (int i = 0; i < NUM_ITERATIONS; i++) {
        int index = barrier.await();
        ToolkitSortedSet sortedSet = toolkit.getSortedSet("sorted_index_set_" + i, Integer.class);
        assertTrue(sortedSet.add(index));
      }

      int departureIndex = syncBarrier.await(); // Check conditions when all parties have arrived.
      if (departureIndex == 0) {
        for (int i = 0; i < NUM_ITERATIONS; i++) {
          assertEquals(getParticipantCount(), toolkit.getSortedSet("sorted_index_set_" + i,
                                                                   Integer.class).size());
          toolkit.getSortedSet("sorted_index_set_" + i, Integer.class).destroy();
        }
      }
    }
  }
}
