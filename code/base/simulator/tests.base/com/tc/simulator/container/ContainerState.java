/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.container;

import com.tc.simulator.listener.OutputListener;
import com.tc.simulator.listener.StatsListener;
import com.tc.simulator.listener.StatsListenerFactory;

import java.util.Properties;

public interface ContainerState extends StatsListenerFactory {

  public String getContainerId();

  public OutputListener newOutputListener();

  public StatsListener newStatsListener(Properties properties);

}