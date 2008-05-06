/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcsimulator.listener;

import com.tc.simulator.listener.ListenerProvider;
import com.tc.simulator.listener.MutationCompletionListener;
import com.tc.simulator.listener.OutputListener;
import com.tc.simulator.listener.ResultsListener;
import com.tc.simulator.listener.StatsListener;
import com.tc.simulator.listener.StatsListenerFactory;

import java.util.Properties;

public final class ApplicationListenerProvider implements ListenerProvider {
  private final OutputListener             outputListener;
  private final ResultsListener            resultsListener;
  private final StatsListenerFactory       statsListenerFactory;
  private final MutationCompletionListener mutationCompletionListener;

  public ApplicationListenerProvider(OutputListener ol, ResultsListener rl, MutationCompletionListener mcl,
                                     StatsListenerFactory statsListenerFactory) {
    outputListener = ol;
    resultsListener = rl;
    mutationCompletionListener = mcl;
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

  public MutationCompletionListener getMutationCompletionListener() {
    return mutationCompletionListener;
  }

}