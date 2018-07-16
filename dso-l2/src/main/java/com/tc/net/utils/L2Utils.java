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
package com.tc.net.utils;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class L2Utils {
  private static final int MAX_DEFAULT_COMM_THREADS = 16;
  private static final int MAX_DEFAULT_STAGE_THREADS = 16;
  private static final int MAX_ENTITY_PROCESSOR_THREADS = 128;
  public static final long MIN_COMMS_DIRECT_MEMORY_REQUIREMENT = 4 * 1024 * 1024;  // 4MiB
  public static final long MAX_COMMS_DIRECT_MEMORY_REQUIREMENT = 256 * 1024 * 1024; // 256MiB

  public static int getOptimalCommWorkerThreads() {
    // We currently set the number of comm threads to the number of available processors.
    // This is further limited by MAX_DEFAULT_COMM_THREADS to ensure that the number selected doesn't go so far as to expose
    // other performance bottlenecks within the application logic.
    // Note that this value is only the result of observations and reasonable behavior but may need to be tweaked, in the
    // future.
    int halfProcs = Runtime.getRuntime().availableProcessors() >> 1;
    if (halfProcs == 0) halfProcs = 1;
    int def = Math.min(halfProcs, MAX_DEFAULT_COMM_THREADS);
    return TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_TCCOM_WORKERTHREADS, def);
  }
  
  public static int getOptimalStageWorkerThreads() {
    // We currently set the number of stage worker threads to the number of available processors.
    // This is further limited by MAX_DEFAULT_STAGE_THREADS to ensure that the number selected doesn't go so far as to
    // expose other performance bottlenecks within the application logic.
    // Note that this value is only the result of observations and reasonable behavior but may need to be tweaked, in the
    // future.
    int def = Math.min(Runtime.getRuntime().availableProcessors(), MAX_DEFAULT_STAGE_THREADS);
    return TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_SEDA_STAGE_WORKERTHREADS, def);
  }

  /**
   * Calculates the optimal number of worker threads for the apply stage.
   * <p/>{@code l2.seda.apply.stage.threads} configuration property overrides this value.
   *
   * @param usesDisk if uses disk then less computational
   * @return the optimal number of threads for the apply stage
   */
  public static int getOptimalApplyStageWorkerThreads(boolean usesDisk) {
    final int cpus = Runtime.getRuntime().availableProcessors();
    // in restartable mode wait/compute time ratio is low due to disk I/O
    final int threadsCount = (usesDisk) ? calculateOptimalThreadsCount(cpus, 30, 70, 0.75)
        : calculateOptimalThreadsCount(cpus, 0, 100, 0.75);
    return TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.ENTITY_PROCESSOR_THREADS,
        Math.min(threadsCount, MAX_ENTITY_PROCESSOR_THREADS));
  }

  /**
   * Calculates the optimal number of worker threads based on the formula below:
   * <p/> {@code N_threads = N_cpu * U_cpu * (1 + W/C)}, where
   * <ul>
   * <li>{@code N_cpu} - number of CPUs.</li>
   * <li>{@code U_cpu} - target CPU utilization, 0 <= U_cpu <= 1.</li>
   * <li>{@code W/C} - ratio of wait time to compute time. Empirical value.</li>
   * </ul>
   * <p/> If the task is pure computational, that is {@code wait = 0},
   * one more thread added to compensate any possible pauses (e.g. page fault).
   *
   * @return the optimal number of threads
   */
  public static int calculateOptimalThreadsCount(int cpus, long wait, long compute,
                                                 double targetUtilization) {
    Assert.assertTrue(compute > 0);
    Assert.assertTrue(wait >= 0);
    Assert.assertTrue(cpus > 0);
    Assert.assertTrue(targetUtilization > 0 && targetUtilization <= 1);

    final BigDecimal cpusCount = new BigDecimal(cpus);
    final BigDecimal waitTime = new BigDecimal(wait);
    final BigDecimal computeTime = new BigDecimal(compute);
    final BigDecimal utilization = new BigDecimal(targetUtilization);

    int threadsCount = cpusCount.multiply(utilization).multiply(BigDecimal.ONE
        .add(waitTime.divide(computeTime, 1, RoundingMode.HALF_UP))).setScale(0, RoundingMode.HALF_UP).intValue();
    return (wait == 0) ? threadsCount + 1 : threadsCount;
  }
}
