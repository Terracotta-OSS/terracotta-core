package com.tc.services;

import java.io.Serializable;

import com.tc.services.LocalMonitoringProducer.ActivePipeWrapper;


/**
 * Manages the cache and delayed dispatch of best-efforts data passed to IMonitoringProducer while the server is in passive
 *  mode.
 * Note that this interface is synchronized to ensure safe interaction with internal threads.
 */
public class BestEffortsMonitoring {
  private static final int BATCH_SIZE = 10;

  private ActivePipeWrapper activeWrapper;
  private long[] consumerIDCache;
  private String[] keyCache;
  private Serializable[] valueCache;
  private int nextIndex;


  public synchronized void attachToNewActive(ActivePipeWrapper activeWrapper) {
    // Note that it is possible that there already is an active and this is replacing it.
    this.activeWrapper = activeWrapper;
    initializeCache();
  }

  public synchronized void pushBestEfforts(long consumerID, String name, Serializable data) {
    // Put these in the cache.
    this.consumerIDCache[this.nextIndex] = consumerID;
    this.keyCache[this.nextIndex] = name;
    this.valueCache[this.nextIndex] = data;
    this.nextIndex += 1;
    
    // See if we need to flush the cache.
    if (BATCH_SIZE == this.nextIndex) {
      this.activeWrapper.pushBestEffortsBatch(this.consumerIDCache, this.keyCache, this.valueCache);
      initializeCache();
    }
  }


  private void initializeCache() {
    this.consumerIDCache = new long[BATCH_SIZE];
    this.keyCache = new String[BATCH_SIZE];
    this.valueCache = new Serializable[BATCH_SIZE];
    this.nextIndex = 0;
  }
}
