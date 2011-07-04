/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import com.tc.stats.counter.CounterConfig;

public class StatsConfig {

  private final String        name;
  private final CounterConfig counterConfig;

  public StatsConfig(String name, CounterConfig cc) {
    this.name = name;
    this.counterConfig = cc;
  }

  public String getStatsName() {
    return name;
  }

  public CounterConfig getCounterConfig() {
    return counterConfig;
  }
}