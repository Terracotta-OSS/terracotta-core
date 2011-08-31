/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.simulator.listener;

import java.util.Properties;

public class MockListenerProvider implements ListenerProvider {

  public OutputListener  outputListener;
  public ResultsListener resultsListener;
  public StatsListener   statsListener;

  public OutputListener getOutputListener() {
    return outputListener;
  }

  public ResultsListener getResultsListener() {
    return this.resultsListener;
  }

  public StatsListener getStatsListener() {
    return statsListener;
  }

  public StatsListener newStatsListener(Properties properties) {
    return statsListener;
  }

  public MutationCompletionListener getMutationCompletionListener() {
    throw new AssertionError("This method needs to be implemented");
  }

}