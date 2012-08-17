/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.CallableWaiter;
import org.terracotta.express.tests.DSOMBean;
import org.terracotta.express.tests.ServerMBeanRetriever;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.test.jmx.TestHandlerMBean;
import com.tc.test.setup.GroupsData;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class ClusteredMapDeleteValueOnRemoveTestClient extends ClientBase {
  public static final int                                NODE_COUNT                       = 2;
  private static final int                               OBJECT_COUNT_STABILIZE_TIMEOUT   = 5 * 60 * 1000;
  private static final int                               OBJECT_COUNT_STABILIZE_INTERVAL  = 10 * 1000;
  private static final int                               OBJECT_COUNT_STABILIZE_THRESHOLD = 5;

  private static final Random                            random                           = new Random();
  private static final DateFormat                        DATE_FORMAT                      = new SimpleDateFormat(
                                                                                                                 "hh:mm:ss.SSS");
  private static final int                               SEGMENT_RUNTIME                  = 10 * 1000;

  private final Map<Integer, List<ServerMBeanRetriever>> mBeanRetrievers                  = new HashMap<Integer, List<ServerMBeanRetriever>>();
  // Since we're avoiding DGC's to make an attempt at getting a closer count, need to keep track of the number of times
  // the barrier has been used. It generates a "Generation" object every time the clients joins on a barrier, so use
  // this count to keep track of it locally.
  private int                                            barrierCount                     = 0;
  private int                                            nodeId                           = -1;
  private ToolkitStore<String, String>                   cdsm;

  public ClusteredMapDeleteValueOnRemoveTestClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new ClusteredMapDeleteValueOnRemoveTestClient(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    initmBeanRetrievers();
    try {
      nodeId = getBarrierForAllClients().await();
      cdsm = toolkit.getStore("testStore", String.class);
      runTest();
    } catch (Throwable t) {
      if (nodeId == 0) {
        if (!attemptClusterDump()) {
          info("Failed to take a cluster dump.");
        }
        if (cdsm != null) {
          info("CDSM Size: " + cdsm.size());
        }
      }
      throw new RuntimeException(t);
    }
    awaitBarrier();
  }

  private void initmBeanRetrievers() {
    int stripe = 0;
    TestHandlerMBean testControlMbean = getTestControlMbean();

    for (GroupsData groupData : testControlMbean.getGroupsData()) {
      List<ServerMBeanRetriever> stripeRetrievers = new ArrayList<ServerMBeanRetriever>();
      for (int i = 0; i < groupData.getServerCount(); i++) {
        int jmxPort = groupData.getJmxPort(i);
        System.out.println("Adding jmxPort " + jmxPort);
        stripeRetrievers.add(new ServerMBeanRetriever("localhost", jmxPort));
      }
      mBeanRetrievers.put(stripe, stripeRetrievers);
      stripe++;
    }
  }

  private boolean attemptClusterDump() {
    for (List<ServerMBeanRetriever> mBeanRetrieverList : mBeanRetrievers.values()) {
      for (ServerMBeanRetriever mBeanRetriever : mBeanRetrieverList) {
        try {
          mBeanRetriever.getL2DumperMBean().dumpClusterState();
          // Sucessfully dumped, so we're getting out here
          return true;
        } catch (Exception e2) {
          // ignore it and move on
        }
      }
    }
    return false;
  }

  private void runTest() throws Exception {
    Map<Integer, Integer> initialObjectCount = waitForAllLiveObjectCountsToStabilize();
    info("Initial object count: " + initialObjectCount);
    awaitBarrier();

    info("Putting and removing items.");
    for (long start = System.nanoTime(), key = 0; TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < SEGMENT_RUNTIME; key++) {
      Assert.assertNull(cdsm.put(getMyKey(key), getMyValue(key)));
      Assert.assertNotNull(cdsm.remove(getMyKey(key)));
    }
    awaitBarrier();

    waitForLiveObjectCounts(initialObjectCount, 0);
    awaitBarrier();

    info("Putting some items.");
    for (long key = 0; key < 1000; key++) {
      Assert.assertNull(cdsm.put(getMyKey(key), getMyValue(key)));
    }
    awaitBarrier();

    info("Removing another node's items.");
    for (long key = 0; key < 1000; key++) {
      Assert.assertNotNull(cdsm.remove(getOtherNodeKey(key)));
    }
    awaitBarrier();

    waitForLiveObjectCounts(initialObjectCount, 0);
    awaitBarrier();

    info("Replacing an item");
    Assert.assertNull(cdsm.put(getMyKey(0), getMyValue(0)));
    for (long start = System.nanoTime(); TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < SEGMENT_RUNTIME;) {
      Assert.assertNotNull(cdsm.put(getMyKey(0), getMyValue(0)));
    }
    awaitBarrier();

    waitForLiveObjectCounts(initialObjectCount, NODE_COUNT);
    awaitBarrier();

    info("Populating the map.");
    for (long start = System.nanoTime(), key = 1; TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < SEGMENT_RUNTIME; key++) {
      Assert.assertNull(cdsm.put(getMyKey(key), getMyValue(key)));
    }
    awaitBarrier();

    if (nodeId == 0) {
      info("Clearing the map.");
      cdsm.clear();
    }
    awaitBarrier();

    waitForLiveObjectCounts(initialObjectCount, 0);

    awaitBarrier();

    info("Putting from one node and unlocked getting from another");
    for (long start = System.nanoTime(); TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < SEGMENT_RUNTIME;) {
      if (nodeId == 0) {
        cdsm.put("key", "value" + System.nanoTime());
      } else {
        cdsm.get("key");
      }
    }
    awaitBarrier();
  }

  private void waitForLiveObjectCounts(final Map<Integer, Integer> expectedCounts, final int offset) throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        // Initiate an inline dgc (this won't hit every stripe every time, but it should be sufficient since we need to
        // loop anyways).
        String key = String.valueOf(random.nextInt());
        cdsm.put(key, "boo");
        cdsm.remove(key);

        for (Entry<Integer, List<ServerMBeanRetriever>> stripe : mBeanRetrievers.entrySet()) {
          boolean stripeIsDown = true;
          for (ServerMBeanRetriever stripeMember : stripe.getValue()) {
            try {
              int actualObjectCount = stripeMember.getDSOMBean().getLiveObjectCount();
              int expectedObjectCount = expectedCounts.get(stripe.getKey());
              int expectedLowerBound = (int) ((expectedObjectCount * 0.9) + offset - barrierCount);
              int expectedUpperBound = (int) ((expectedObjectCount * 1.1) + barrierCount + offset);
              info("Current live object count for stripe " + stripe.getKey() + ": " + actualObjectCount
                   + " expectedLowerBound: " + expectedLowerBound + " expectedUpperBound: " + expectedUpperBound);
              if (!(actualObjectCount >= expectedLowerBound && actualObjectCount <= expectedUpperBound)) { return false; }
              stripeIsDown = false;
              break; // count is in range, so we're done with this stripe
            } catch (Exception e) {
              // Ignore it and try another
            }
          }
          if (stripeIsDown) { return false; }
        }
        return true;
      }
    }, OBJECT_COUNT_STABILIZE_TIMEOUT, OBJECT_COUNT_STABILIZE_INTERVAL);
  }

  private Map<Integer, Integer> waitForAllLiveObjectCountsToStabilize() throws Exception {
    final Map<Integer, Integer> stableCounts = new HashMap<Integer, Integer>();
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      private int stableLength = 0;

      @Override
      public Boolean call() throws Exception {
        for (Entry<Integer, List<ServerMBeanRetriever>> stripe : mBeanRetrievers.entrySet()) {
          boolean stripeUp = false;
          for (ServerMBeanRetriever mBeanRetriever : stripe.getValue()) {
            if (!stableCounts.containsKey(stripe.getKey())) {
              stableCounts.put(stripe.getKey(), 0);
            }
            try {
              DSOMBean dsoMBean = mBeanRetriever.getDSOMBean();
              stripeUp = true; // track whether we're actually able to access the stripe
              if (dsoMBean.getLiveObjectCount() == stableCounts.get(stripe.getKey())) {
                infoOnlyOnNode(0, "Stripe " + stripe.getKey() + " stable at " + dsoMBean.getLiveObjectCount());
                break;
              } else {
                stableLength = 0;
                stableCounts.put(stripe.getKey(), dsoMBean.getLiveObjectCount());
                return false;
              }
            } catch (Exception e) {
              // moving on to the next server.
            }
          }
          if (!stripeUp) {
            infoOnlyOnNode(0, "Stripe " + stripe.getKey() + " is not up. Resetting.");
            return false;
          }
        }
        stableLength++;
        return stableLength >= OBJECT_COUNT_STABILIZE_THRESHOLD;
      }
    }, OBJECT_COUNT_STABILIZE_TIMEOUT, OBJECT_COUNT_STABILIZE_INTERVAL);
    return stableCounts;
  }

  private String getOtherNodeKey(long keyId) {
    return "key-" + ((nodeId + 1) % NODE_COUNT) + "-" + keyId;
  }

  private String getMyKey(long keyId) {
    return "key-" + nodeId + "-" + keyId;
  }

  private String getMyValue(long keyId) throws IllegalArgumentException {
    return Long.toString(keyId);
  }

  private void infoOnlyOnNode(final int infoNodeId, final String msg) {
    if (infoNodeId == nodeId) {
      info(msg);
    }
  }

  private void info(final String msg) {
    System.out.println(DATE_FORMAT.format(new Date()) + " Node[" + nodeId + "] " + msg);
  }

  private int awaitBarrier() throws InterruptedException, BrokenBarrierException {
    barrierCount++;
    return getBarrierForAllClients().await();
  }
}
