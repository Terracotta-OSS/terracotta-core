/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import org.junit.Test;

import com.tc.net.GroupID;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public class RootsHolderTest extends TestCase {

  private final CyclicBarrier              barrier              = new CyclicBarrier(2);
  private final static int                 NUM_GROUPS           = 16;
  private final static int                 INSERTIONS_PER_GROUP = 5;
  private final static String              NAME_ZERO            = getNameOID(1, 0);
  private final static GroupID             GID_ZERO             = new GroupID(0);
  private final AtomicReference<Throwable> assertErrorMarker    = new AtomicReference<Throwable>();

  private static class RootsHolderTestRunnable implements Runnable {
    private final RootsHolder rootsHolder;
    private final int         oidOffset;
    private final CyclicBarrier barrier;
    private final AtomicReference<Throwable> errorMarker;

    public RootsHolderTestRunnable(RootsHolder rootsHolder, CyclicBarrier barrier, int oidOffset,
                                   AtomicReference<Throwable> errorMarker) {
      this.rootsHolder = rootsHolder;
      this.oidOffset = oidOffset;
      this.barrier = barrier;
      this.errorMarker = errorMarker;
    }

    @Override
    public void run() {
      for (int i = 0; i < NUM_GROUPS; i++) {
        for (int j = 1; j <= INSERTIONS_PER_GROUP; j++) {
          // Choosing a unique object id
          int oid = i * INSERTIONS_PER_GROUP + j + oidOffset;
          rootsHolder.addRoot(getNameOID(oid, i), new ObjectID(oid, i));
        }
      }

      try {
        int index = barrier.await();
        assertEquals(2 * NUM_GROUPS * INSERTIONS_PER_GROUP, rootsHolder.size());
        if (index == 0) {
          markLookupInProgress();
        }
        barrier.await();
        if (index == 1) {
          verifyMarked();
          unmarkLookupInProgress();
        }
        barrier.await();
        if (index == 0) {
          verifyUnmarked();
        }
      } catch (Throwable throwable) {
        System.out.println(throwable.getMessage());
        throwable.printStackTrace();
        errorMarker.set(throwable);
      }
    }

    private void markLookupInProgress() {
      rootsHolder.markRootLookupInProgress(NAME_ZERO, GID_ZERO);
    }

    private void verifyMarked() throws Throwable {
      assertEquals(rootsHolder.isLookupInProgress(NAME_ZERO, GID_ZERO), true);
    }

    private void unmarkLookupInProgress() {
      rootsHolder.unmarkRootLookupInProgress(NAME_ZERO, GID_ZERO);
    }

    private void verifyUnmarked() throws Throwable {
      assertEquals(rootsHolder.isLookupInProgress(NAME_ZERO, GID_ZERO), false);
    }
  }

  private static String getNameOID(int oid, int gid) {
    return "(oid=" + oid + ", gid=" + gid + ")";
  }

  @Test
  public void testRootsHolder() throws InterruptedException {
    GroupID[] groupIds = new GroupID[NUM_GROUPS];
    for (int i = 0; i < NUM_GROUPS; i++) {
      groupIds[i] = new GroupID(i);
    }

    RootsHolder rootsHolder = new RootsHolder(groupIds);
    Thread tAlpha = new Thread(new RootsHolderTestRunnable(rootsHolder, barrier, 0, assertErrorMarker));
    Thread tBeta = new Thread(new RootsHolderTestRunnable(rootsHolder, barrier, NUM_GROUPS * INSERTIONS_PER_GROUP,
                                                          assertErrorMarker));

    tAlpha.start();
    tBeta.start();

    tAlpha.join();
    tBeta.join();

    assertNull(assertErrorMarker.get());
    assertEquals(rootsHolder.getGroupIDForRoot(NAME_ZERO), rootsHolder.getGroupIDForRoot(NAME_ZERO));
    assertEquals(new ObjectID(1), rootsHolder.getRootIDForName(NAME_ZERO, GID_ZERO));
  }
}
