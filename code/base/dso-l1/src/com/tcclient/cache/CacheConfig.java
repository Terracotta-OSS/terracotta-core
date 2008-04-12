/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.cache;

import com.tc.object.bytecode.Manager;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;

/**
 * Cache configuration.  This is a shared object so only one should exist.  This config is shared 
 * across all nodes involved in the cache and is immutable.  
 */
public class CacheConfig {

  // Configuration
  private final String cacheName;  
  private final long maxIdleTimeoutSeconds;     // max time cache element can live without access
  private final long maxTTLSeconds;             // max time cache element can live regardless of access
  private final long invalidatorSleepSeconds;   // wait time between eviction cycles
  private final Expirable callback;             // callback on expire() 

  // Derived from config
  private final long maxIdleTimeoutMillis;      // same as above, but in milliseconds
  private final long maxTTLMillis;              // same as above, but in milliseconds
  
  // Configuration from ehcache properties
  private final int concurrency;                // number of maps in the cache
  private final int evictorPoolSize;            // number of evictor threads, must be <= concurrency
  private final boolean globalEvictionEnabled;  
  private final int globalEvictionFrequency;    // # of times to run local eviction before doing global
  private final int numOfChunks;                // break global eviction into chunks
  private final long restMillis;                // rest between global eviction of each chunk
  private final boolean isLoggingEnabled;       // CacheDataStore logging
  private final boolean isEvictorLoggingEnabled;// CacheInvalidationTimer logging
  
  public CacheConfig(String cacheName, 
                     long maxIdleTimeoutSeconds, 
                     long maxTTLSeconds, 
                     long invalidatorSleepSeconds,
                     Expirable callback,   
                     // Start ehcache-related properties 
                     int concurrency,     
                     int evictorPoolSize,
                     boolean globalEvictionEnabled,
                     int globalEvictionFrequency,
                     boolean isLoggingEnabled,
                     boolean isEvictorLoggingEnabled,
                     int numOfChunks,
                     long restMillis) {
    
    this.cacheName = cacheName;
    this.maxIdleTimeoutSeconds = maxIdleTimeoutSeconds;
    this.maxTTLSeconds = maxTTLSeconds;
    this.invalidatorSleepSeconds = invalidatorSleepSeconds;
    this.callback = callback;    
    
    this.maxIdleTimeoutMillis = maxIdleTimeoutSeconds * 1000;
    this.maxTTLMillis = maxTTLSeconds * 1000;

    this.concurrency = concurrency;
    this.evictorPoolSize = evictorPoolSize;
    this.globalEvictionEnabled = globalEvictionEnabled;
    this.globalEvictionFrequency = globalEvictionFrequency;
    this.isLoggingEnabled = isLoggingEnabled;
    this.isEvictorLoggingEnabled = isEvictorLoggingEnabled;
    this.numOfChunks = numOfChunks;
    this.restMillis = restMillis;
  }

  /**
   * Extract ehcache config properties from manager.
   */
  public CacheConfig(String cacheName, 
                     long maxIdleTimeoutSeconds, 
                     long maxTTLSeconds, 
                     long invalidatorSleepSeconds,
                     Expirable callback,
                     Manager manager) {
    
    this.cacheName = cacheName;
    this.maxIdleTimeoutSeconds = maxIdleTimeoutSeconds;
    this.maxTTLSeconds = maxTTLSeconds;
    this.invalidatorSleepSeconds = invalidatorSleepSeconds;
    this.callback = callback;    
    
    this.maxIdleTimeoutMillis = maxIdleTimeoutSeconds * 1000;
    this.maxTTLMillis = maxTTLSeconds * 1000;

    // Load ehcache properties
    TCProperties props = manager.getTCProperites();
    TCProperties ehcacheProperties = props.getPropertiesFor("ehcache");
    int concurrencyProp = ehcacheProperties.getInt("concurrency");
    if (concurrencyProp <= 1) {
      this.concurrency = 1;
      this.evictorPoolSize = 1;
    } else {
      this.concurrency = concurrencyProp;
      int evictorPoolSizeProp = ehcacheProperties.getInt("evictor.pool.size");
      if (evictorPoolSizeProp > this.concurrency) {
        this.evictorPoolSize = this.concurrency;
      } else {
        this.evictorPoolSize = evictorPoolSizeProp;
      }
    }
    this.globalEvictionEnabled = ehcacheProperties.getBoolean("global.eviction.enable");
    this.globalEvictionFrequency = ehcacheProperties.getInt("global.eviction.frequency");

    this.isLoggingEnabled = props.getBoolean(TCPropertiesConsts.EHCAHCE_LOGGING_ENABLED, false);

    TCProperties ehcacheProperies = props.getPropertiesFor("ehcache.global.eviction");
    this.numOfChunks = ehcacheProperies.getInt("segments");
    this.restMillis = ehcacheProperies.getLong("rest.timeMillis");
    
    this.isEvictorLoggingEnabled = props.getBoolean(TCPropertiesConsts.EHCAHCE_EVICTOR_LOGGING_ENABLED, false);
  }

  public int getConcurrency() {
    return concurrency;
  }

  public int getEvictorPoolSize() {
    return evictorPoolSize;
  }

  public boolean isGlobalEvictionEnabled() {
    return globalEvictionEnabled;
  }

  public int getGlobalEvictionFrequency() {
    return globalEvictionFrequency;
  }

  public boolean isLoggingEnabled() {
    return isLoggingEnabled;
  }

  public int getNumOfChunks() {
    return numOfChunks;
  }

  public long getRestMillis() {
    return restMillis;
  }

  public String getCacheName() {
    return cacheName;
  }

  public long getMaxIdleTimeoutSeconds() {
    return maxIdleTimeoutSeconds;
  }

  public long getMaxTTLSeconds() {
    return maxTTLSeconds;
  }

  public long getInvalidatorSleepSeconds() {
    return invalidatorSleepSeconds;
  }

  public Expirable getCallback() {
    return callback;
  }

  public long getMaxIdleTimeoutMillis() {
    return maxIdleTimeoutMillis;
  }

  public long getMaxTTLMillis() {
    return maxTTLMillis;
  }
  
  public int getStoresPerInvalidator() {
    return concurrency / evictorPoolSize;
  }

  public boolean isEvictorLoggingEnabled() {
    return isEvictorLoggingEnabled;
  }
}
