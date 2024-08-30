/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;


public class L2UtilsTest {
  /**
   * A test to verify that the default thread counts respond as expected.
   */
  @Test
  public void testVerifyDefaultThreadCounts() {
    int processorCount = Runtime.getRuntime().availableProcessors();
    
    int commThreadCount = L2Utils.getOptimalCommWorkerThreads();
    Assert.assertTrue(commThreadCount > 0);
    // We currently bound the number of comm threads at 16 (may change in the future).
    Assert.assertTrue((commThreadCount == processorCount) || (commThreadCount <= 16));
    
    int stageThreadCount = L2Utils.getOptimalStageWorkerThreads();
    Assert.assertTrue(stageThreadCount > 0);
    // We currently bound the number of stage threads at 16 (may change in the future).
    Assert.assertTrue((stageThreadCount == processorCount) || (stageThreadCount <= 16));
  }

  /**
   * A test to verify that the default thread counts can be tweaked with TCProperties.
   */
  @Test
  public void testVerifyTCPropertiesThreadCounts() {
    TCProperties properties = TCPropertiesImpl.getProperties();
    int originalCommThreadCount = L2Utils.getOptimalCommWorkerThreads();
    int originalStageThreadCount = L2Utils.getOptimalStageWorkerThreads();
    
    // WARNING:  setting L2_TCCOM_WORKERTHREADS and L2_SEDA_STAGE_WORKERTHREADS cannot be undone!
    properties.setProperty(TCPropertiesConsts.L2_TCCOM_WORKERTHREADS, "" + (originalCommThreadCount + 1));
    properties.setProperty(TCPropertiesConsts.L2_SEDA_STAGE_WORKERTHREADS, "" + (originalStageThreadCount + 1));
    Assert.assertTrue((originalCommThreadCount + 1) == L2Utils.getOptimalCommWorkerThreads());
    Assert.assertTrue((originalStageThreadCount + 1) == L2Utils.getOptimalStageWorkerThreads());
  }

  @Test
  public void testShouldCorrectlyCalculateOptimalThreadsCount() {
    assertEquals(6, L2Utils.calculateOptimalThreadsCount(4, 30, 70, 1.0));
    assertEquals(20, L2Utils.calculateOptimalThreadsCount(2, 90, 10, 1.0));
    assertEquals(1, L2Utils.calculateOptimalThreadsCount(1, 30, 70, 1.0));
    assertEquals(2, L2Utils.calculateOptimalThreadsCount(1, 0, 100, 1.0));
    assertEquals(22, L2Utils.calculateOptimalThreadsCount(16, 30, 70, 1.0));
    assertEquals(112, L2Utils.calculateOptimalThreadsCount(80, 30, 70, 1.0));
    assertEquals(84, L2Utils.calculateOptimalThreadsCount(80, 30, 70, 0.75));
  }

  @Test(expected = AssertionError.class)
  public void testShouldThrowIAEOnInvalidUtilization() {
    L2Utils.calculateOptimalThreadsCount(4, 30, 70, 0.0);
  }

  @Test(expected = AssertionError.class)
  public void testShouldThrowIAEOnInvalidCpusCount() {
    L2Utils.calculateOptimalThreadsCount(0, 30, 70, 1.0);
  }

  @Test(expected = AssertionError.class)
  public void testShouldThrowIAEOnInvalidCompute() {
    L2Utils.calculateOptimalThreadsCount(4, 30, 0, 1.0);
  }
}
