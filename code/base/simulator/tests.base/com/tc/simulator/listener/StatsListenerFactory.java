/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.listener;


import java.util.Properties;

public interface StatsListenerFactory {
  public StatsListener newStatsListener(Properties properties);
}
