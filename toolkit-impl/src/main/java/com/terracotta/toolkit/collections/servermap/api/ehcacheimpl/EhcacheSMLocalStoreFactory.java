/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap.api.ehcacheimpl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheInitializationHelper;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.EhcacheInitializationHelper;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.constructs.classloader.InternalClassLoaderAwareCache;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.terracotta.InternalEhcache;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreConfig;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;

public class EhcacheSMLocalStoreFactory implements ServerMapLocalStoreFactory {

  private final CacheManager defaultCacheManager;

  public EhcacheSMLocalStoreFactory(CacheManager defaultCacheManager) {
    this.defaultCacheManager = defaultCacheManager;
  }

  @Override
  public <K, V> ServerMapLocalStore<K, V> getOrCreateServerMapLocalStore(ServerMapLocalStoreConfig config) {
    InternalEhcache localStoreCache = getOrCreateEhcacheLocalCache(config);

    return (ServerMapLocalStore<K, V>) new EhcacheSMLocalStore(localStoreCache);
  }

  private synchronized InternalEhcache getOrCreateEhcacheLocalCache(ServerMapLocalStoreConfig config) {
    InternalEhcache ehcache;
    CacheManager cacheManager = getOrCreateCacheManager(config);
    final String localCacheName = "local_shadow_cache_for_" + cacheManager.getName() + "_" + config.getLocalStoreName();
    ehcache = (InternalEhcache) cacheManager.getEhcache(localCacheName);
    if (ehcache == null) {
      ehcache = createCache(localCacheName, config);
      new EhcacheInitializationHelper(cacheManager).initializeEhcache(ehcache);
    }
    return ehcache;
  }

  private synchronized CacheManager getOrCreateCacheManager(ServerMapLocalStoreConfig config) {
    String localStoreManagerName = config.getLocalStoreManagerName();
    if (localStoreManagerName == null || "".equals(localStoreManagerName.trim())) {
      // use default cache manager when no name is specified
      return defaultCacheManager;
    }
    CacheManager cacheManager = CacheInitializationHelper
        .getInitializingCacheManager(config.getLocalStoreManagerName());
    if (cacheManager != null) { return cacheManager; }

    cacheManager = CacheManager.getCacheManager(config.getLocalStoreManagerName());
    if (cacheManager == null) {
      cacheManager = CacheManager.create(new Configuration().name(config.getLocalStoreManagerName()));
    }
    return cacheManager;
  }

  private static InternalEhcache createCache(String cacheName, ServerMapLocalStoreConfig config) {
    CacheConfiguration cacheConfig = new CacheConfiguration(cacheName, 0).overflowToDisk(false)
        .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.CLOCK);

    // wire up config
    if (config.getMaxCountLocalHeap() > 0) {
      // this is due to the meta mapping we put in the shadow cache
      cacheConfig.setMaxEntriesLocalHeap(config.getMaxCountLocalHeap() * 2 + 1);
    }

    if (config.getMaxBytesLocalHeap() > 0) {
      cacheConfig.setMaxBytesLocalHeap(config.getMaxBytesLocalHeap());
    }

    cacheConfig.setOverflowToOffHeap(config.isOverflowToOffheap());
    if (config.isOverflowToOffheap()) {
      long maxBytesLocalOffHeap = config.getMaxBytesLocalOffheap();
      if (maxBytesLocalOffHeap > 0) {
        cacheConfig.setMaxBytesLocalOffHeap(maxBytesLocalOffHeap);
      }
    }

    if (config.isPinnedInLocalMemory()) {
      // pin elements in local shadow cache.
      cacheConfig.pinning(new PinningConfiguration().store(PinningConfiguration.Store.LOCALMEMORY));
    }

    return new InternalClassLoaderAwareCache(new Cache(cacheConfig), ServerMapLocalStoreFactory.class.getClassLoader());
  }
}