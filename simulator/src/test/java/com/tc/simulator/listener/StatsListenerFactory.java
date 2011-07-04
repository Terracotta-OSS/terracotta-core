/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.listener;


import java.util.Properties;

public interface StatsListenerFactory {
  public StatsListener newStatsListener(Properties properties);
}
