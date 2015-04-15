/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.async.impl;

import com.tc.async.api.StageMonitor.Analysis;
import com.tc.text.StringFormatter;
import com.tc.util.concurrent.ThreadUtil;

import junit.framework.TestCase;

public class StageMonitorImplTest extends TestCase {
  public void tests() throws Exception {
    StageMonitorImpl sm = new StageMonitorImpl("name", new StringFormatter());

    sm.eventBegin(10);
    ThreadUtil.reallySleep(100);
    sm.eventBegin(1);
    sm.eventBegin(0);
    ThreadUtil.reallySleep(100);
    sm.eventBegin(10);

    System.out.println(sm.dumpAndFlush());

    sm.eventBegin(10);
    ThreadUtil.reallySleep(1000);
    sm.eventBegin(10);

    System.out.println(sm.dumpAndFlush());

    for (int i = 0; i < 10000; i++) {
      sm.eventBegin(i);
    }

    System.out.println(sm.dumpAndFlush());
  }

  public void testAnalyze() throws Exception {
    StageMonitorImpl sm = new StageMonitorImpl("name", new StringFormatter());

    Analysis an = sm.analyze();
    assertEquals(Integer.valueOf(0), an.getEventCount());
    // assertEquals(new Double(0), an.getEventsPerSecond());
    assertEquals(Integer.valueOf(-1), an.getMinQueueDepth());
    assertEquals(Integer.valueOf(0), an.getMaxQueueDepth());
    assertEquals(Double.valueOf(-1), an.getAvgQueueDepth());

    sm.eventBegin(10);
    ThreadUtil.reallySleep(100);
    sm.eventBegin(20);
    an = sm.analyze();

    assertEquals(Integer.valueOf(2), an.getEventCount());
    String elapsed = String.valueOf(an.getElapsedTime().longValue());
    assertTrue(elapsed, an.getElapsedTime().longValue() >= 100);
    assertTrue(elapsed, an.getElapsedTime().longValue() < 2500);
    assertEquals(Double.valueOf(1000 * an.getEventCount().doubleValue() / an.getElapsedTime().doubleValue()),
                 an.getEventsPerSecond());
    assertEquals(Integer.valueOf(10), an.getMinQueueDepth());
    assertEquals(Integer.valueOf(20), an.getMaxQueueDepth());

  }

}
