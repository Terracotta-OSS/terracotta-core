package com.tc.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;

import com.tc.services.LocalMonitoringProducer.ActivePipeWrapper;
import com.tc.util.Assert;


/**
 * Manages the cache and delayed dispatch of best-efforts data passed to IMonitoringProducer while the server is in passive
 *  mode.
 * Note that this interface is synchronized to ensure safe interaction with internal threads.
 */
public class BestEffortsMonitoring {
  // Currently, we will only flush messages every so often, not based on the size of the cache.  This means that the data remains fresher.
  private static final int MESSAGE_FLUSH_FREQUENCY = 10;

  private ActivePipeWrapper activeWrapper;
  private final Map<Long, Map<String, Serializable>> bestEffortsCache;
  // We only want to push every MESSAGE_FLUSH_FREQUENCY messages, so keep track of how may we received since last flush.
  private int messagesSinceLastFlush;


  public BestEffortsMonitoring() {
    this.bestEffortsCache = new HashMap<Long, Map<String, Serializable>>();
  }

  public synchronized void flushAfterActivePromotion(PlatformServer thisServer , TerracottaServiceProviderRegistry globalRegistry) {
    // Walk each consumerID, looking up their registries, and flushing all entries to the implementation.
    for (Map.Entry<Long, Map<String, Serializable>> perConsumerEntry : this.bestEffortsCache.entrySet()) {
      IStripeMonitoring collector = globalRegistry.subRegistry(perConsumerEntry.getKey()).getService(new BasicServiceConfiguration<IStripeMonitoring>(IStripeMonitoring.class));
      // NOTE:  We assert that there _is_ a registry for IStripeMonitoring if we received this call.
      Assert.assertNotNull(collector);
      for (Map.Entry<String, Serializable> entry : perConsumerEntry.getValue().entrySet()) {
        collector.pushBestEffortsData(thisServer, entry.getKey(), entry.getValue());
      }
    }
    // We can now drop this (gratuitous but makes it clear we are done).
    this.bestEffortsCache.clear();
  }

  public synchronized void attachToNewActive(ActivePipeWrapper activeWrapper) {
    // Note that it is possible that there already is an active and this is replacing it.
    this.activeWrapper = activeWrapper;
    
    // See if we need to flush, now that we have an attached active.
    flushCacheAndReset();
  }

  public synchronized void pushBestEfforts(long consumerID, String name, Serializable data) {
    // We lazily build the cache.
    if (!this.bestEffortsCache.containsKey(consumerID)) {
      this.bestEffortsCache.put(consumerID, new HashMap<String, Serializable>());
    }
    
    // Update the cache.
    Map<String, Serializable> map = this.bestEffortsCache.get(consumerID);
    map.put(name, data);
    this.messagesSinceLastFlush += 1;
    
    // Flush the cache if there is anything here.
    flushCacheAndReset();
  }


  private void flushCacheAndReset() {
    // NOTE:  This must be called under lock!
    if ((null != this.activeWrapper) && (this.messagesSinceLastFlush >= MESSAGE_FLUSH_FREQUENCY)) {
      // First, traverse the tree to see how many messages we are going to send in this batch.
      int messagesInBatch = 0;
      for (Map.Entry<Long, Map<String, Serializable>> entry : this.bestEffortsCache.entrySet()) {
        messagesInBatch += entry.getValue().size();
      }
      
      // Serialize the cache.
      long[] consumerIDs = new long[messagesInBatch];
      String[] keys = new String[messagesInBatch];
      Serializable[] values = new Serializable[messagesInBatch];
      int index = 0;
      for (Map.Entry<Long, Map<String, Serializable>> entry : this.bestEffortsCache.entrySet()) {
        long consumerID = entry.getKey();
        for (Map.Entry<String, Serializable> mapEntry : entry.getValue().entrySet()) {
          consumerIDs[index] = consumerID;
          keys[index] = mapEntry.getKey();
          values[index] = mapEntry.getValue();
          index += 1;
        }
      }
      // Clear it.
      this.bestEffortsCache.clear();
      
      // Push and reset our counter.
      this.activeWrapper.pushBestEffortsBatch(consumerIDs, keys, values);
      this.messagesSinceLastFlush = 0;
    }
  }
}
