package com.tc.services;

import java.io.Serializable;

import com.tc.services.LocalMonitoringProducer.ActivePipeWrapper;


/**
 * Manages the cache and delayed dispatch of best-efforts data passed to IMonitoringProducer while the server is in passive
 *  mode.
 * Note that this interface is synchronized to ensure safe interaction with internal threads.
 */
public class BestEffortsMonitoring {
  private ActivePipeWrapper activeWrapper;


  public synchronized void attachToNewActive(ActivePipeWrapper activeWrapper) {
    // Note that it is possible that there already is an active and this is replacing it.
    this.activeWrapper = activeWrapper;
  }

  public synchronized void pushBestEfforts(long consumerID, String name, Serializable data) {
    // We currently don't cache anything - just pass this straight through.
    this.activeWrapper.pushBestEfforts(consumerID, name, data);
  }
}
