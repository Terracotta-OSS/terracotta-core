/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.CallableWaiter;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Callable;

public class ClusteredMapInvalidateTestClient extends ClientBase {
  private static final int             INVALIDATION_TIMEOUT = 60 * 1000;

  public static final int              NUM_ELEMENTS         = 5000;
  public static final int              NODE_COUNT           = 2;
  private final DateFormat             dateFormat           = new SimpleDateFormat();
  private ToolkitBarrier               barrier;
  private ToolkitStore<String, byte[]> testMap;
  private int                          nodeId;

  public static void main(String[] args) {
    new ClusteredMapInvalidateTestClient(args).run();
  }

  public ClusteredMapInvalidateTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    ToolkitStoreConfigBuilder clusteredMapConfig = new ToolkitStoreConfigBuilder();
    clusteredMapConfig.concurrency(1).consistency(Consistency.EVENTUAL);
    barrier = toolkit.getBarrier("barrier", NODE_COUNT);
    nodeId = this.barrier.await();
    testMap = toolkit.getStore("someMapName", clusteredMapConfig.build(), null);
    runClient();
  }

  void runClient() throws Exception {
    this.barrier.await();

    if (nodeId == 1) {
      info("Start populating cache.");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        testMap.putNoReturn(getKey(i), getValueFor(i));
      }
      info("Finished populating cache.");
    }

    barrier.await();

    if (nodeId == 1) {
      info("Waiting for the other node to verify data.");
    } else {
      info("Verifying data put by the other node.");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        checkElementValueEqualWithinTimeout(testMap, getKey(i), getValueFor(i));
      }
      info("Finished verifying data.");
    }

    barrier.await();

    if (nodeId == 1) {
      info("Waiting for other node to update values.");
    } else {
      info("Updating cache values.");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        testMap.putNoReturn(getKey(i), getValueFor(i + NUM_ELEMENTS));
      }
      info("Finished updating cache values.");
    }
    barrier.await();

    if (nodeId == 1) {
      info("Verifying updated cache values.");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        checkElementValueEqualWithinTimeout(testMap, getKey(i), getValueFor(i + NUM_ELEMENTS));
      }
      info("Finished updating cache values.");

      info("Removing and updating cache values.");
      for (int i = 0; i < NUM_ELEMENTS * 2; i++) {
        if (i < NUM_ELEMENTS) {
          testMap.removeNoReturn(getKey(i));
        } else {
          testMap.putNoReturn(getKey(i), getValueFor(i + NUM_ELEMENTS * 2));
        }
      }
      info("Finished removing and updating cache values.");
    } else {
      info("Waiting for the other node to verify and update cache values.");
    }
    barrier.await();

    if (nodeId == 1) {
      info("Waiting for the other node to verify removed and updated cache values.");
    } else {
      info("Verifying removed and updated cache values.");
      for (int i = 0; i < NUM_ELEMENTS * 2; i++) {
        if (i < NUM_ELEMENTS) {
          checkElementNullWithinTimeout(testMap, getKey(i));
        } else {
          checkElementValueEqualWithinTimeout(testMap, getKey(i), getValueFor(i + NUM_ELEMENTS * 2));
        }
      }
      info("Finished verifying removed and updated cache values.");
    }
  }

  private void info(String msg) {
    System.out.println(dateFormat.format(new Date()) + " [Node-" + nodeId + "] " + msg);
  }

  public void checkElementValueEqualWithinTimeout(final ToolkitStore<String, byte[]> cdsm, final String key,
                                                  final byte[] expectedValue) throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      public Boolean call() throws Exception {
        byte[] actualValue = cdsm.get(key);
        return actualValue != null && Arrays.equals(actualValue, expectedValue);
      }
    }, INVALIDATION_TIMEOUT);
  }

  public void checkElementNullWithinTimeout(final ToolkitStore<String, byte[]> cdsm, final String key) throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      public Boolean call() throws Exception {
        return cdsm.get(key) == null;
      }
    }, INVALIDATION_TIMEOUT);
  }

  private String getKey(int i) {
    return "key-" + i;
  }

  private byte[] getValueFor(int v) {
    byte[] rv = new byte[4];

    rv[0] = (byte) ((v >>> 24) & 0xFF);
    rv[1] = (byte) ((v >>> 16) & 0xFF);
    rv[2] = (byte) ((v >>> 8) & 0xFF);
    rv[3] = (byte) ((v >>> 0) & 0xFF);

    return rv;
  }

}
