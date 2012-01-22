/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
