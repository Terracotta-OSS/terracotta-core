/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;


public class PassthroughMonitoringProducer implements PassthroughImplementationProvidedServiceProvider {
  private final PassthroughServerProcess serverProcess;
  // We only keep the cached tree root until we become active.
  // (the tree is per-consumerID).
  private Map<Long, CacheNode> cachedTreeRoot;
  // The serverInfoToken is set when either we become active or are attached to a specific active as a passive.
  private PlatformServer serverInfoToken;
  // The reference to our upstream active monitoring producer for when we need to plumb data there.
  private PassthroughMonitoringProducer activeMonitoringProducer;

  public PassthroughMonitoringProducer(PassthroughServerProcess serverProcess) {
    this.serverProcess = serverProcess;
    this.cachedTreeRoot = new HashMap<Long, CacheNode>();
  }

  public void didBecomeActive(PlatformServer serverInfo) {
    Assert.assertTrue(null != serverInfo);
    this.serverInfoToken = serverInfo;
    // Get the service for the platform's consumerID.
    final IStripeMonitoring platformMonitoring = getUnderlyingService(null, null, ServiceProvider.PLATFORM_CONSUMER_ID, null);
    // Make sure that we are actually running with a monitoring service.
    if (null != platformMonitoring) {
      platformMonitoring.serverDidBecomeActive(this.serverInfoToken);
      // Now, replay the cache.
      for (Map.Entry<Long, CacheNode> entry : this.cachedTreeRoot.entrySet()) {
        final IStripeMonitoring entityMonitoring = getUnderlyingService(null, null, entry.getKey(), null);
        Assert.assertTrue(null != entityMonitoring);
        walkCacheChildren(entityMonitoring, new String[0], entry.getValue().children);
      }
    }
    this.cachedTreeRoot = null;
  }

  @Override
  public <T> T getService(String entityClassName, String entityName, final long consumerID, DeferredEntityContainer container, ServiceConfiguration<T> configuration) {
    T service = null;
    Class<T> serviceType = configuration.getServiceType();
    if (serviceType.equals(IMonitoringProducer.class)) {
      final IStripeMonitoring underlying = getUnderlyingService(entityClassName, entityName, consumerID, container);
      if (null != underlying) {
        // If we are caching, make sure that we have a node.
        if ((null != this.cachedTreeRoot) && !this.cachedTreeRoot.containsKey(consumerID)) {
          this.cachedTreeRoot.put(consumerID, new CacheNode(null));
        }
        service = serviceType.cast(new IMonitoringProducer(){
          @Override
          public boolean addNode(String[] parents, String name, Serializable value) {
            return addNodeFromShim(consumerID, underlying, parents, name, value);
          }
          @Override
          public boolean removeNode(String[] parents, String name) {
            return removeNodeFromShim(consumerID, underlying, parents, name);
          }
          @Override
          public void pushBestEffortsData(String name, Serializable data) {
            pushBestEffortsFromShim(consumerID, underlying, name, data);
          }
        });
      }
    }
    return service;
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    Set<Class<?>> set = new HashSet<Class<?>>();
    set.add(IMonitoringProducer.class);
    return set;
  }

  public synchronized void setUpstreamActive(PassthroughMonitoringProducer activeMonitoringProducer, PlatformServer passiveServerInfoToken) {
    this.activeMonitoringProducer = activeMonitoringProducer;
    this.serverInfoToken = passiveServerInfoToken;
    // First step is to tell the active that we have attached.
    this.activeMonitoringProducer.passiveDidJoinCluster(this.serverInfoToken);
    // We now want to trigger a flush of our cached state.
    // NOTE:  This is very similar to what we do on promote to active but is copy-paste instead of common since the output
    //  pipe was much of the specialization, anyway, and made the path less clear.
    for (Map.Entry<Long, CacheNode> entry : this.cachedTreeRoot.entrySet()) {
      long consumerID = entry.getKey();
      walkCacheChildrenToActive(consumerID, new String[0], entry.getValue().children);
    }
  }

  /**
   * Called during shutdown of the server where this producer lives.
   */
  public synchronized void serverDidStop() {
    if (null != this.activeMonitoringProducer) {
      this.activeMonitoringProducer.passiveDidLeaveCluster(this.serverInfoToken);
    }
  }


  private IStripeMonitoring getUnderlyingService(String entityClassName, String entityName, long consumerID, DeferredEntityContainer container) {
    PassthroughServiceRegistry registry = PassthroughMonitoringProducer.this.serverProcess.createServiceRegistryForInternalConsumer(entityClassName, entityName, consumerID, container);
    final IStripeMonitoring underlying = registry.getService(new BasicServiceConfiguration<IStripeMonitoring>(IStripeMonitoring.class));
    return underlying;
  }

  private synchronized boolean addNodeFromShim(long consumerID, IStripeMonitoring underlyingCollector, String[] parents, String name, Serializable value) {
    boolean didStore = false;
    // First off, see if we have a cache - this determines if we are in active or passive mode.
    if (null != this.cachedTreeRoot) {
      // This means we are passive.
      CacheNode parentNode = findParent(consumerID, parents);
      if (null != parentNode) {
        parentNode.children.put(name, new CacheNode(value));
        didStore = true;
      }
      if (null != this.activeMonitoringProducer) {
        this.activeMonitoringProducer.addNodeFromPassive(this.serverInfoToken, consumerID, parents, name, value);
      }
    } else {
      // This means we are active so just pass it through.
      didStore = underlyingCollector.addNode(this.serverInfoToken, parents, name, value);
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
        didRemove = (null != removed);
      }
      if (null != this.activeMonitoringProducer) {
        this.activeMonitoringProducer.removeNodeFromPassive(this.serverInfoToken, consumerID, parents, name);
      }
    } else {
      // This means we are active so just pass it through.
      didRemove = underlyingCollector.removeNode(this.serverInfoToken, parents, name);
    }
    return didRemove;
  }

  private synchronized void pushBestEffortsFromShim(long consumerID, IStripeMonitoring underlyingCollector, String name, Serializable data) {
    if (null != this.cachedTreeRoot) {
      // We are the passive.
      // We don't cache anything, just send it right on the active.
      // Note:  It might be a better handling of the emulated nature to randomly drop messages, here.
      if (null != this.activeMonitoringProducer) {
        this.activeMonitoringProducer.pushBestEffortsFromPassive(this.serverInfoToken, consumerID, name, data);
      }
    } else {
      // We are the active so just push this through.
      underlyingCollector.pushBestEffortsData(this.serverInfoToken, name, data);
    }
  }

  private CacheNode findParent(long consumerID, String[] parents) {
    CacheNode oneNode = this.cachedTreeRoot.get(consumerID);
    for (int i = 0; (null != oneNode)  && (i < parents.length); ++i) {
      oneNode = oneNode.children.get(parents[i]);
    }
    CacheNode parentNode = null;
    if (null != oneNode) {
      parentNode = oneNode;
    }
    return parentNode;
  }

  private void walkCacheChildren(IStripeMonitoring entityMonitoring, String[] parents, Map<String, CacheNode> nodeChildren) {
    for (Map.Entry<String, CacheNode> child : nodeChildren.entrySet()) {
      walkCacheNode(entityMonitoring, parents, child.getKey(), child.getValue());
    }
  }

  private void walkCacheNode(IStripeMonitoring entityMonitoring, String[] parents, String nodeName, CacheNode node) {
    // Make sure we aren't walking the root node.
    Assert.assertTrue(null != nodeName);
    
    entityMonitoring.addNode(this.serverInfoToken, parents, nodeName, node.data);
    String[] newParents = new String[parents.length + 1];
    System.arraycopy(parents, 0, newParents, 0, parents.length);
    newParents[parents.length] = nodeName;
    walkCacheChildren(entityMonitoring, newParents, node.children);
  }

  /**
   * Like waskCacheChildren but sends the data to the upstream active, instead.
   * (called before sync, when a new active is elected).
   */
  private void walkCacheChildrenToActive(long consumerID, String[] parents, Map<String, CacheNode> nodeChildren) {
    for (Map.Entry<String, CacheNode> child : nodeChildren.entrySet()) {
      walkCacheNodeToActive(consumerID, parents, child.getKey(), child.getValue());
    }
  }

  /**
   * Like waskCacheNode but sends the data to the upstream active, instead.
   * (called before sync, when a new active is elected).
   */
  private void walkCacheNodeToActive(long consumerID, String[] parents, String nodeName, CacheNode node) {
    // Make sure we aren't walking the root node.
    Assert.assertTrue(null != nodeName);
    
    this.activeMonitoringProducer.addNodeFromPassive(this.serverInfoToken, consumerID, parents, nodeName, node.data);
    String[] newParents = new String[parents.length + 1];
    System.arraycopy(parents, 0, newParents, 0, parents.length);
    newParents[parents.length] = nodeName;
    walkCacheChildrenToActive(consumerID, newParents, node.children);
  }


  /***** Entry-points for messages coming from downstream passives *****/
  // Note that these are all synchronized since they come in from another thread (which sort of emulates how they can come
  //  in other threads, for the underlying service implementation).
  private synchronized void passiveDidJoinCluster(PlatformServer passiveInfo) {
    final IStripeMonitoring platformMonitoring = getUnderlyingService(null, null, ServiceProvider.PLATFORM_CONSUMER_ID, null);
    if (null != platformMonitoring) {
      platformMonitoring.serverDidJoinStripe(passiveInfo);
    }
  }

  private synchronized void passiveDidLeaveCluster(PlatformServer passiveInfo) {
    final IStripeMonitoring platformMonitoring = getUnderlyingService(null, null, ServiceProvider.PLATFORM_CONSUMER_ID, null);
    if (null != platformMonitoring) {
      platformMonitoring.serverDidLeaveStripe(passiveInfo);
    }
  }

  private synchronized void addNodeFromPassive(PlatformServer passiveInfo, long consumerID, String[] parents, String nodeName, Serializable nodeData) {
    final IStripeMonitoring platformMonitoring = getUnderlyingService(null, null, consumerID, null);
    if (null != platformMonitoring) {
      platformMonitoring.addNode(passiveInfo, parents, nodeName, nodeData);
    }
  }

  private synchronized void removeNodeFromPassive(PlatformServer passiveInfo, long consumerID, String[] parents, String nodeName) {
    final IStripeMonitoring platformMonitoring = getUnderlyingService(null, null, consumerID, null);
    if (null != platformMonitoring) {
      platformMonitoring.removeNode(passiveInfo, parents, nodeName);
    }
  }

  private synchronized void pushBestEffortsFromPassive(PlatformServer passiveInfo, long consumerID, String name, Serializable data) {
    final IStripeMonitoring platformMonitoring = getUnderlyingService(null, null, consumerID, null);
    if (null != platformMonitoring) {
      platformMonitoring.pushBestEffortsData(passiveInfo, name, data);
    }
  }


  private static class CacheNode {
    public final Serializable data;
    public final Map<String, CacheNode> children;
    
    public CacheNode(Serializable data) {
      this.data = data;
      this.children = new HashMap<String, CacheNode>();
    }
  }
}
