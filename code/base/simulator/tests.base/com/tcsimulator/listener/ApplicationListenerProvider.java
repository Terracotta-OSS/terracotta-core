/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator.listener;

import com.tc.simulator.listener.ListenerProvider;
import com.tc.simulator.listener.OutputListener;
import com.tc.simulator.listener.ResultsListener;
import com.tc.simulator.listener.StatsListener;
import com.tc.simulator.listener.StatsListenerFactory;

import java.util.Properties;

public final class ApplicationListenerProvider implements ListenerProvider {
  private final OutputListener       outputListener;
  private final ResultsListener      resultsListener;
  private final StatsListenerFactory statsListenerFactory;

  public ApplicationListenerProvider(OutputListener ol, ResultsListener rl, StatsListenerFactory statsListenerFactory) {
    this.outputListener = ol;
    this.resultsListener = rl;
    this.statsListenerFactory = statsListenerFactory;
  }

  public OutputListener getOutputListener() {
    return outputListener;
  }

  public ResultsListener getResultsListener() {
    return resultsListener;
  }

  public StatsListener newStatsListener(Properties properties) {
    return statsListenerFactory.newStatsListener(properties);
  }

}