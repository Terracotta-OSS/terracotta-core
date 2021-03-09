/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.testing.master;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.testing.api.ITestClusterConfiguration;
import org.terracotta.testing.logging.VerboseManager;
import org.terracotta.utilities.test.net.EphemeralPorts;
import org.terracotta.utilities.test.net.PortManager;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;
import org.junit.Ignore;

@SuppressWarnings("deprecation")
public class AbstractHarnessEntryTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHarnessEntryTest.class);

  private static final PortManager PORT_MANAGER = PortManager.getInstance();

  @Rule
  public final TestName testName = new TestName();

  @Test(timeout = 10000)
  public void testChooseRandomPortRangeSingle() throws Exception {
    LOGGER.info("************************************************************" +
        "\nRunning test {}" +
        "\n************************************************************",
        testName.getMethodName());

    HarnessEntry entry = new HarnessEntry();
    int firstPort = entry.chooseRandomPortRange(1);
    assertThat(PORT_MANAGER.reserve(firstPort), is(Optional.empty()));

    /*
     * Discard the reference to HarnessEntry to permit collection.
     */
    ReferenceQueue<HarnessEntry> queue = new ReferenceQueue<>();
    WeakReference<HarnessEntry> harnessEntryReference = new WeakReference<>(entry, queue);
    entry = null;

    /*
     * Now attempt to reserve the port again; a weak reference to the HarnessEntry should permit
     * the PortRef to be reclaimed.
     */
    awaitEnqueue(queue);    // await HarnessEntry move to weakly referenced
    reservePort(firstPort);
  }

  @Test(timeout = 10000) @Ignore("test times out on mac")
  public void testChooseRandomPortRangeMultiple() throws Exception {
    LOGGER.info("************************************************************" +
            "\nRunning test {}" +
            "\n************************************************************",
        testName.getMethodName());

    int portCount = 6;
    HarnessEntry entry = new HarnessEntry();
    int firstPort = entry.chooseRandomPortRange(portCount);

    for (int port = firstPort, i = 0; i < portCount; port++, i++) {
      assertThat(PORT_MANAGER.reserve(port), is(Optional.empty()));
    }

    /*
     * Discard the reference to HarnessEntry to permit collection.
     */
    ReferenceQueue<HarnessEntry> queue = new ReferenceQueue<>();
    WeakReference<HarnessEntry> harnessEntryReference = new WeakReference<>(entry, queue);
    entry = null;

    /*
     * Now attempt to reserve the port again; a weak reference to the HarnessEntry should permit
     * the PortRef to be reclaimed.
     */
    awaitEnqueue(queue);    // await HarnessEntry move to weakly referenced
    for (int port = firstPort, i = 0; i < portCount; port++, i++) {
      reservePort(port);
    }
  }

  /**
   * Tests the operation of {@link AbstractHarnessEntry#chooseRandomPortRange(int)} when
   * desired ranges are obstructed.  This test relies on internal knowledge of
   * {@link PortManager}.
   */
  @Test @Ignore("test takes too long")
  public void testChooseRandomPortRangeBlocked() throws Exception {
    LOGGER.info("************************************************************" +
            "\nRunning test {}" +
            "\n************************************************************",
        testName.getMethodName());

    int portCount = 20;

    HarnessEntry entry = new HarnessEntry();

    /*
     * Determine the port range from which PortManger will allocate ports.
     * Based on the "current" implementation, PortManager will NOT allocate
     * a port under 1025 or within the range returned by EphemeralPorts.getRange().
     */
    BitSet blockedPorts = new BitSet(65536);
    blockedPorts.set(0, 1025);
    EphemeralPorts.Range ephemeralPorts = EphemeralPorts.getRange();
    blockedPorts.set(ephemeralPorts.getLower(), 1 + ephemeralPorts.getUpper());

    LOGGER.info("Blocking every {}th port", portCount);
    List<PortManager.PortRef> blockingPorts = new ArrayList<>();
    try {
      /*
       * Now (attempt to) reserve every 'portCount'th port to prevent use of
       * that range for allocation by chooseRandomPortRange(portCount).
       */
      for (int allocablePort = blockedPorts.nextClearBit(0);
           allocablePort < blockedPorts.size();
           allocablePort = allocablePort + portCount) {
        int blockerPort = allocablePort - 1;
        if (!blockedPorts.get(blockerPort)) {
          try {
            PORT_MANAGER.reserve(blockerPort).ifPresent(blockingPorts::add);
          } catch (IllegalArgumentException | IllegalStateException ignored) {
          }
        }
      }

      /*
       * Now try to allocate a range of 'portCount' ports.
       */
      LOGGER.info("Attempting allocation of {} ports with all ranges blocked", portCount);
      try {
        entry.chooseRandomPortRange(portCount);
        fail("Expecting IllegalStateException");
      } catch (IllegalStateException e) {
        assertThat(e.getMessage(), containsString("Failed to obtain"));
      }

      /*
       * Retry blocking port release & chooseRandomPortRange until blocked ports
       * are exhausted or chooseRandomPortRange succeeds.
       */
      int unblockedRangeCount = 0;
      while (!blockingPorts.isEmpty()) {
        /*
         * Release some of the ranges to permit the allocation to succeed.
         */
        LOGGER.info("Releasing 10 blocked ports to retry allocation");
        Collections.shuffle(blockingPorts);
        ListIterator<PortManager.PortRef> iterator = blockingPorts.listIterator();
        for (int i = 0; i < 10 && iterator.hasNext(); i++) {
          PortManager.PortRef blockedPort = iterator.next();
          blockedPort.close();
          iterator.remove();
          unblockedRangeCount++;
        }

        /*
         * Now attempt the allocation again ...
         */
        LOGGER.info("Attempting allocation of {} ports with {} ranges unblocked", portCount, unblockedRangeCount);
        try {
          int firstPort = entry.chooseRandomPortRange(portCount);
          LOGGER.info("Allocation of {} consecutive ports from {} successful", portCount, firstPort);
          break;
        } catch (IllegalStateException e) {
          if (!e.getMessage().contains("Failed to obtain")) {
            throw e;
          } else {
            LOGGER.info("Allocation of {} consecutive ports failed; retrying", portCount);
          }
        }
      }

      if (blockingPorts.isEmpty()) {
        fail("chooseRandomPortRange did not successfully allocate " + portCount + " ports even with no ports blocked");
      }

    } finally {
      LOGGER.info("Releasing all blocked ranges");
      blockingPorts.forEach(PortManager.PortRef::close);
    }
  }

  private void reservePort(int port) throws InterruptedException {
    Optional<PortManager.PortRef> portRef;
    do {
      portRef = PORT_MANAGER.reserve(port);
      if (!portRef.isPresent()) {
        System.gc();
        TimeUnit.MILLISECONDS.sleep(1000);
      }
    } while (!portRef.isPresent());
    portRef.ifPresent(PortManager.PortRef::close);
  }

  private void awaitEnqueue(ReferenceQueue<HarnessEntry> queue) throws InterruptedException {
    while (queue.poll() == null) {
      System.gc();
      TimeUnit.MILLISECONDS.sleep(1000);
    }
  }

  private static final class HarnessEntry extends AbstractHarnessEntry<Config> {
    @Override
    protected void runOneConfiguration(VerboseManager verboseManager, DebugOptions debugOptions, CommonHarnessOptions commonHarnessOptions, Config runConfiguration) {
      throw new UnsupportedOperationException("HarnessEntry.runOneConfiguration not implemented");
    }
  }

  private static final class Config implements ITestClusterConfiguration {
    @Override
    public String getName() {
      return null;
    }
  }
}