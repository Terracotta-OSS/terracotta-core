/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.listener;


import java.util.Properties;



public class MockListenerProvider implements ListenerProvider {

  public OutputListener outputListener;
  public ResultsListener resultsListener;
  public StatsListener statsListener;
  
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

}