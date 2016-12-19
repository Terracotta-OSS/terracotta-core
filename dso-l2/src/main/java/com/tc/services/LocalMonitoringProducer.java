package com.tc.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;

import com.tc.classloader.BuiltinService;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ServerID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.util.Assert;


/**
 * Manages the redirection of data over IStripeMonitoring.
 * 
 * When the server is active, the instance passes all local or remote data directly to the underlying IStripeMonitoring.
 * When the server is passive, the instance caches all local data, internally, and copies it to the active.
 * When the server changes from passive to active, it copies its local data to the underlying IStripeMonitoring and stops
 *  caching.
 * When the server is passive and is told to send its local data to the new active, it copies its cached data to the new
 *  active.
 * 
 * NOTE:  Due to the way that this is directly referenced by ManagementTopologyEventCollector, we expect the consumerID
 *  instance to be long-lived (we can't rely on it being re-requested after active promotion, as we can for most services).
 *  In the future, we may have a better solution to this problem by treating the platform's "fake" entity as something more
 *  real, and accessing it through that.
 * 
 * XXX: The synchronization through this class should be re-thought since it introduces bottlenecks and brittle code which
 *  probably has better solutions.
 */
@BuiltinService
public class LocalMonitoringProducer implements ImplementationProvidedServiceProvider {
  private static final TCLogger LOGGER = TCLogging.getLogger(LocalMonitoringProducer.class);
  private final TerracottaServiceProviderRegistry globalRegistry;
  private final PlatformServer thisServer;
  private final Map<ServerID, PlatformServer> otherServers;
  private ActivePipeWrapper activeWrapper;
  // We only keep the cached tree root until we become active.
  // (the tree is per-consumerID).
  private Map<Long, CacheNode> cachedTreeRoot;
  private BestEffortsMonitoring bestEfforts;

  public LocalMonitoringProducer(TerracottaServiceProviderRegistry globalRegistry, PlatformServer thisServer, SingleThreadedTimer timer) {
    this.globalRegistry = globalRegistry;
    this.thisServer = thisServer;
    this.otherServers = new HashMap<ServerID, PlatformServer>();
    this.cachedTreeRoot = new HashMap<Long, CacheNode>();
    this.bestEfforts = new BestEffortsMonitoring(timer);
  }

  public PlatformServer getLocalServerInfo() {
    return this.thisServer;
  }

  @Override
  public synchronized <T> T getService(long consumerID, ManagedEntity owningEntity, ServiceConfiguration<T> configuration) {
    Class<T> type = configuration.getServiceType();
    Assert.assertEquals(type, IMonitoringProducer.class);
    // If we are caching, make sure that we have a node.
    if ((null != this.cachedTreeRoot) && !this.cachedTreeRoot.containsKey(consumerID)) {
      this.cachedTreeRoot.put(consumerID, new CacheNode(null));
    }
    IStripeMonitoring underlyingCollector = this.globalRegistry.subRegistry(consumerID).getService(new BasicServiceConfiguration<IStripeMonitoring>(IStripeMonitoring.class));
    T service = null;
    if (null != underlyingCollector) {
      service = type.cast(new IMonitoringProducer() {
        @Override
        public boolean addNode(String[] parents, String name, Serializable value) {
          return addNodeFromShim(consumerID, underlyingCollector, parents, name, value);
        }
        @Override
        public boolean removeNode(String[] parents, String name) {
          return removeNodeFromShim(consumerID, underlyingCollector, parents, name);
        }
        @Override
        public void pushBestEffortsData(String name, Serializable data) {
          pushBestEffortsFromShim(consumerID, underlyingCollector, name, data);
        }
      });
    }
    return service;
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(IMonitoringProducer.class);
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    // Do nothing.
  }

  @Override
  public synchronized void serverDidBecomeActive() {
 //  avoid this notification,  it happens too early.  the method below will be called directly in the correct sequence
  }
  
  public synchronized void serverIsActive() {
    // Tell the ID0 instance that the server is active.
    IStripeMonitoring platformCollector = this.globalRegistry.subRegistry(ServiceProvider.PLATFORM_CONSUMER_ID).getService(new BasicServiceConfiguration<IStripeMonitoring>(IStripeMonitoring.class));
    
    // Note that the underlying collector will be present for all or none of the consumerIDs.
    if (null != platformCollector) {
      platformCollector.serverDidBecomeActive(this.thisServer);
      
      // Pass our cached state into the underlying services and then drop our cache and pipe to the active
      for (Map.Entry<Long, CacheNode> entry : this.cachedTreeRoot.entrySet()) {
        long consumerID = entry.getKey();
        IStripeMonitoring underlyingCollector = this.globalRegistry.subRegistry(consumerID).getService(new BasicServiceConfiguration<IStripeMonitoring>(IStripeMonitoring.class));
        walkCacheChildren(new String[0], entry.getValue().children, new CacheWalker() {
          @Override
          public void didEnterNode(String[] parents, String name, Serializable value) {
            underlyingCollector.addNode(LocalMonitoringProducer.this.thisServer, parents, name, value);
          }});
      }
      
      // Flush any remaining best-efforts data to the collector.
      this.bestEfforts.flushAfterActivePromotion(this.thisServer, this.globalRegistry);
    }
    
    this.cachedTreeRoot = null;
    this.bestEfforts = null;
    this.activeWrapper = null;
  }

  /**
   * Called on a passive entity when an active has been elected.  The receiver is expected to send its cached data to this new active and send any updates there, going forward.
   * 
   * @param activeWrapper
   */
  public synchronized void sendToNewActive(ActivePipeWrapper activeWrapper) {
    // Store the new wrapper.
    this.activeWrapper = activeWrapper;
    
    // Send our cached state to the new active.
    if (this.cachedTreeRoot != null) {
      for (Map.Entry<Long, CacheNode> entry : this.cachedTreeRoot.entrySet()) {
        long consumerID = entry.getKey();
        walkCacheChildren(new String[0], entry.getValue().children, new CacheWalker() {
          @Override
          public void didEnterNode(String[] parents, String name, Serializable value) {
            // Send this to the active.
            LocalMonitoringProducer.this.activeWrapper.addNode(consumerID, parents, name, value);
          }});
      }
      this.bestEfforts.attachToNewActive(this.activeWrapper);
    } else {
//  split brain.  one of the actives will die shortly.
    }
  }

  public void serverDidJoinStripe(ServerID sender, PlatformServer platformServer) {
    //  WARNING:  It is possible to get multiple copies if servers rapidly join and leave the group, always update
    PlatformServer oldValue = this.otherServers.put(sender, platformServer);
    LOGGER.warn("multiple copies of server information are being reported.old=" + oldValue + " new=" + platformServer);
    
    // Notify the platform collector.
    IStripeMonitoring platformCollector = this.globalRegistry.subRegistry(ServiceProvider.PLATFORM_CONSUMER_ID).getService(new BasicServiceConfiguration<IStripeMonitoring>(IStripeMonitoring.class));
    if (null != platformCollector) {
      platformCollector.serverDidJoinStripe(platformServer);
    }
  }

  public void serverDidLeaveStripe(ServerID nodeID) {
    // WARNING:  It is possible to get this call as a duplicate, via different paths.
    PlatformServer platformServer = this.otherServers.remove(nodeID);
    if (null != platformServer) {
      // Notify the platform collector.
      IStripeMonitoring platformCollector = this.globalRegistry.subRegistry(ServiceProvider.PLATFORM_CONSUMER_ID).getService(new BasicServiceConfiguration<IStripeMonitoring>(IStripeMonitoring.class));
      if (null != platformCollector) {
        platformCollector.serverDidLeaveStripe(platformServer);
      }
    }
  }

  public synchronized void handleRemoteAdd(ServerID sender, long consumerID, String[] parents, String name, Serializable value) {
    // If we are getting these, we MUST be in active mode.
    Assert.assertNull(this.cachedTreeRoot);
    
    IStripeMonitoring underlyingCollector = this.globalRegistry.subRegistry(consumerID).getService(new BasicServiceConfiguration<IStripeMonitoring>(IStripeMonitoring.class));
    if (null != underlyingCollector) {
      PlatformServer sendingServer = this.otherServers.get(sender);
      Assert.assertNotNull(sendingServer);
      underlyingCollector.addNode(sendingServer, parents, name, value);
    }
  }

  public synchronized void handleRemoteRemove(ServerID sender, long consumerID, String[] parents, String name) {
    // If we are getting these, we MUST be in active mode.
    Assert.assertNull(this.cachedTreeRoot);
    
    IStripeMonitoring underlyingCollector = this.globalRegistry.subRegistry(consumerID).getService(new BasicServiceConfiguration<IStripeMonitoring>(IStripeMonitoring.class));
    if (null != underlyingCollector) {
      PlatformServer sendingServer = this.otherServers.get(sender);
      Assert.assertNotNull(sendingServer);
      underlyingCollector.removeNode(sendingServer, parents, name);
    }
  }

  public synchronized void handleRemoteBestEffortsBatch(ServerID sender, long[] consumerIDs, String[] keys, Serializable[] values) {
    // If we are getting these, we MUST be in active mode.
    Assert.assertNull(this.cachedTreeRoot);
    
    for (int i = 0; i < consumerIDs.length; ++i) {
      IStripeMonitoring underlyingCollector = this.globalRegistry.subRegistry(consumerIDs[i]).getService(new BasicServiceConfiguration<IStripeMonitoring>(IStripeMonitoring.class));
      if (null != underlyingCollector) {
        PlatformServer sendingServer = this.otherServers.get(sender);
        Assert.assertNotNull(sendingServer);
        underlyingCollector.pushBestEffortsData(sendingServer, keys[i], values[i]);
      }
    }
  }

  private synchronized boolean addNodeFromShim(long consumerID, IStripeMonitoring underlyingCollector, String[] parents, String name, Serializable value) {
    boolean didStore = false;
    // First off, see if we have a cache - this determines if we are in active or passive mode.
    if (null != LocalMonitoringProducer.this.cachedTreeRoot) {
      // This means we are passive.
      CacheNode parentNode = findParent(consumerID, parents);
      if (null != parentNode) {
        parentNode.children.put(name, new CacheNode(value));
        // This could be cached so we can also send it to any waiting active and return success.
        if (null != LocalMonitoringProducer.this.activeWrapper) {
          LocalMonitoringProducer.this.activeWrapper.addNode(consumerID, parents, name, value);
        }
        didStore = true;
      }
    } else {
      // This means we are active so just pass it through.
      didStore = underlyingCollector.addNode(LocalMonitoringProducer.this.thisServer, parents, name, value);
    }
    return didStore;
  }

  private synchronized boolean removeNodeFromShim(long consumerID, IStripeMonitoring underlyingCollector, String[] parents, String name) {
    boolean didRemove = false;
    // First off, see if we have a cache - this determines if we are in active or passive mode.
    if (null != this.cachedTreeRoot) {
      // This means we are passive.
      CacheNode parentNode = findParent(consumerID, parents);
      if (null != parentNode) {
        CacheNode removed = parentNode.children.remove(name);
        if (null != removed) {
          // This could be cached so we can also send it to any waiting actives and return success.
          if (null != this.activeWrapper) {
            this.activeWrapper.removeNode(consumerID, parents, name);
          }
          didRemove = true;
        }
      }
    } else {
      // This means we are active so just pass it through.
      didRemove = underlyingCollector.removeNode(this.thisServer, parents, name);
    }
    return didRemove;
  }

  private synchronized void pushBestEffortsFromShim(long consumerID, IStripeMonitoring underlyingCollector, String name, Serializable data) {
    if (null != this.bestEfforts) {
      // Pass this to the BestEffortsMonitoring object so it can handle this.
      this.bestEfforts.pushBestEfforts(consumerID, name, data);
    } else {
      // We are the active so just push this through.
      underlyingCollector.pushBestEffortsData(this.thisServer, name, data);
    }
  }

  private CacheNode findParent(long consumerID, String[] parents) {
    CacheNode parentNode = null;
    if (null != this.cachedTreeRoot) {
      CacheNode oneNode = this.cachedTreeRoot.get(consumerID);
      for (int i = 0; (parents != null) && (null != oneNode)  && (i < parents.length); ++i) {
        oneNode = oneNode.children.get(parents[i]);
      }
      if (null != oneNode) {
        parentNode = oneNode;
      }
    }
    return parentNode;
  }

  private void walkCacheChildren(String[] parents, Map<String, CacheNode> nodeChildren, CacheWalker walker) {
    for (Map.Entry<String, CacheNode> child : nodeChildren.entrySet()) {
      walkCacheNode(parents, child.getKey(), child.getValue(), walker);
    }
  }

  private void walkCacheNode(String[] parents, String nodeName, CacheNode node, CacheWalker walker) {
    // Make sure we aren't walking the root node.
    Assert.assertNotNull(nodeName);
    
    walker.didEnterNode(parents, nodeName, node.data);
    String[] newParents = new String[parents.length + 1];
    System.arraycopy(parents, 0, newParents, 0, parents.length);
    newParents[parents.length] = nodeName;
    walkCacheChildren(newParents, node.children, walker);
  }


  public static interface ActivePipeWrapper {
    public void addNode(long consumerID, String[] parents, String name, Serializable value);
    public void removeNode(long consumerID, String[] parents, String name);
    public void pushBestEffortsBatch(long[] consumerIDs, String[] keys, Serializable[] values);
  }


  private static class CacheNode {
    public final Serializable data;
    public final Map<String, CacheNode> children;
    
    public CacheNode(Serializable data) {
      this.data = data;
      this.children = new HashMap<String, CacheNode>();
    }
  }


  private static interface CacheWalker {
    public void didEnterNode(String[] parents, String name, Serializable value);
  }
}
