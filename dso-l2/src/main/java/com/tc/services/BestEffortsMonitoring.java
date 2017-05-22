package com.tc.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceException;
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
  // We will flush 1 second after new data appears.
  // (this is marked public for tests)
  public static final long ASYNC_FLUSH_DELAY_MILLIS = 1000;

  private final ISimpleTimer timer;
  private final Map<Long, Map<String, Serializable>> bestEffortsCache;
  private ActivePipeWrapper activeWrapper;
  private long outstandingTimerToken;


  public BestEffortsMonitoring(ISimpleTimer timer) {
    this.timer = timer;
    this.bestEffortsCache = new HashMap<Long, Map<String, Serializable>>();
  }

  public synchronized void flushAfterActivePromotion(PlatformServer thisServer, TerracottaServiceProviderRegistry globalRegistry) {
    // We no longer care about the timer so clear it, if one exists.
    ensureTimerCancelled();
    
    // Walk each consumerID, looking up their registries, and flushing all entries to the implementation.
    for (Map.Entry<Long, Map<String, Serializable>> perConsumerEntry : this.bestEffortsCache.entrySet()) {
      IStripeMonitoring collector = null;
      try {
        collector = globalRegistry.subRegistry(perConsumerEntry.getKey()).getService(new BasicServiceConfiguration<IStripeMonitoring>(IStripeMonitoring.class));
      } catch (ServiceException e) {
        Assert.fail("Multiple IStripeMonitoring implementations found!");
      }
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
    // In case an active already exists, and we are merely changing the target, we need to ensure that any pending timer is stopped.
    ensureTimerCancelled();
    
    // Note that it is possible that there already is an active and this is replacing it.
    this.activeWrapper = activeWrapper;
    
    // See if we need to flush, now that we have an attached active.
    // We can only flush if there is something here so check that it is even possible (since none of the top-level entries
    //  are empty).
    if (!this.bestEffortsCache.isEmpty()) {
      flushCacheAndReset();
    }
  }

  public synchronized void pushBestEfforts(long consumerID, String name, Serializable data) {
    // We lazily build the cache.
    if (!this.bestEffortsCache.containsKey(consumerID)) {
      this.bestEffortsCache.put(consumerID, new HashMap<String, Serializable>());
    }
    
    // Update the cache.
    Map<String, Serializable> map = this.bestEffortsCache.get(consumerID);
    map.put(name, data);
    
    // Request a flush, if needed.
    requestFlushIfNonePending();
  }

  /**
   * Called by the internal background thread running the timer.
   */
  public synchronized void backgroundThreadFlush() {
    // NOTE:  There is a timing hole here we need to close.  It is possible that the timer was cancelled after it after
    //  started but before it got this lock.  Therefore, we need to make sure that the timer token is still here before we
    //  run.
    if (0 != this.outstandingTimerToken) {
      this.outstandingTimerToken = 0;
      flushCacheAndReset();
    }
  }


  private void requestFlushIfNonePending() {
    // NOTE:  This must be called under lock!
    if ((0 == this.outstandingTimerToken) && (null != this.activeWrapper)) {
      // There is no timer running so request one.
      this.outstandingTimerToken = this.timer.addDelayed(new Runnable(){
        @Override
        public void run() {
          backgroundThreadFlush();
        }}, this.timer.currentTimeMillis() + ASYNC_FLUSH_DELAY_MILLIS);
      Assert.assertTrue(this.outstandingTimerToken > 0);
    }
  }

  private void flushCacheAndReset() {
    // NOTE:  This must be called under lock!
    // This should only ever be called if there is an active wrapper.
    Assert.assertTrue(null != this.activeWrapper);
    // Calling this with a pending timer is an error (if this was called _via_ the timer, it must clear the token before
    //  calling).
    Assert.assertTrue(0 == this.outstandingTimerToken);
    
    // First, traverse the tree to see how many messages we are going to send in this batch.
    int messagesInBatch = 0;
    for (Map.Entry<Long, Map<String, Serializable>> entry : this.bestEffortsCache.entrySet()) {
      messagesInBatch += entry.getValue().size();
    }
    // Note that we currently ensure that this is only called when non-empty.
    Assert.assertTrue(messagesInBatch > 0);
    
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
    
    // Push the batch.
    this.activeWrapper.pushBestEffortsBatch(consumerIDs, keys, values);
  }

  private void ensureTimerCancelled() {
    if (0 != this.outstandingTimerToken) {
      this.timer.cancel(this.outstandingTimerToken);
      // Clear the token so that we can detect it was cancelled, if it already started running.
      this.outstandingTimerToken = 0;
    }
  }
}
