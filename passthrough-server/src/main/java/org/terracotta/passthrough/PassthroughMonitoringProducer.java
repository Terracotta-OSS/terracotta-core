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
  private PlatformServer serverInfoWhileActive;

  public PassthroughMonitoringProducer(PassthroughServerProcess serverProcess) {
    this.serverProcess = serverProcess;
    this.cachedTreeRoot = new HashMap<Long, CacheNode>();
  }

  public void didBecomeActive(PlatformServer serverInfo) {
    Assert.assertTrue(null == this.serverInfoWhileActive);
    Assert.assertTrue(null != serverInfo);
    this.serverInfoWhileActive = serverInfo;
    // Get the service for the platform's consumerID.
    final IStripeMonitoring platformMonitoring = getUnderlyingService(null, null, ServiceProvider.PLATFORM_CONSUMER_ID, null);
    // Make sure that we are actually running with a monitoring service.
    if (null != platformMonitoring) {
      platformMonitoring.serverDidBecomeActive(this.serverInfoWhileActive);
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
            pushBestEffortsFromShim(underlying, name, data);
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


  private IStripeMonitoring getUnderlyingService(String entityClassName, String entityName, long consumerID, DeferredEntityContainer container) {
    PassthroughServiceRegistry registry = PassthroughMonitoringProducer.this.serverProcess.createServiceRegistryForInternalConsumer(entityClassName, entityName, consumerID, container);
    final IStripeMonitoring underlying = registry.getService(new ServiceConfiguration<IStripeMonitoring>() {
      @Override
      public Class<IStripeMonitoring> getServiceType() {
        return IStripeMonitoring.class;
      }
    });
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
    } else {
      // This means we are active so just pass it through.
      didStore = underlyingCollector.addNode(this.serverInfoWhileActive, parents, name, value);
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
    } else {
      // This means we are active so just pass it through.
      didRemove = underlyingCollector.removeNode(this.serverInfoWhileActive, parents, name);
    }
    return didRemove;
  }

  private synchronized void pushBestEffortsFromShim(IStripeMonitoring underlyingCollector, String name, Serializable data) {
    if (null == this.cachedTreeRoot) {
      // We are the active so just push this through.
      underlyingCollector.pushBestEffortsData(this.serverInfoWhileActive, name, data);
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
    
    entityMonitoring.addNode(this.serverInfoWhileActive, parents, nodeName, node.data);
    String[] newParents = new String[parents.length + 1];
    System.arraycopy(parents, 0, newParents, 0, parents.length);
    newParents[parents.length] = nodeName;
    walkCacheChildren(entityMonitoring, newParents, node.children);
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
