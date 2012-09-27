/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap.api.gbimpl;

import org.terracotta.toolkit.cache.ToolkitCacheConfigFields.PinningStore;

import com.tc.gbapi.GBCacheConfig;
import com.tc.gbapi.GBManager;
import com.tc.gbapi.GBManagerConfiguration;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreConfig;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;

import java.util.Map;

public class GBServerMapLocalStoreFactory implements ServerMapLocalStoreFactory {
  private final GBManager manager;

  public GBServerMapLocalStoreFactory() {
    // TODO: does this manager needs some shared config upfront ?
    manager = new GBManager(null, null);
    verify(manager);
    manager.start();
  }

  private void verify(GBManager managerParam) {
    GBManagerConfiguration configuration = managerParam.getConfiguration();

    if (!configuration.cacheConfig().isEmpty()) { throw new IllegalStateException("Restartable is not supported."); }
  }

  @Override
  public synchronized <K, V> ServerMapLocalStore<K, V> getOrCreateServerMapLocalStore(ServerMapLocalStoreConfig config) {
    String name = config.getLocalStoreName();
    GBCacheConfig gbCacheConfig = createConfig(name, config);
    manager.getConfiguration().cacheConfig().put(name, gbCacheConfig);
    
    return new GBServerMapLocalStore(name, manager.getCache(name, null, null), manager);
  }


  private GBCacheConfig createConfig(String cacheName, ServerMapLocalStoreConfig config) {
    GBManagerConfiguration configuration = manager.getConfiguration();
    Map<String, GBCacheConfig<?, ?>> mapOfConfig = configuration.cacheConfig();

    if (mapOfConfig.containsKey(cacheName)) { throw new IllegalArgumentException("Cache " + cacheName
                                                                                 + " already exists locally!!"); }

    GBCacheConfig gbCacheConfig = createConfig(cacheName);

    // wire up config
    if (config.getMaxCountLocalHeap() > 0) {
      // this is due to the meta mapping we put in the shadow cache
      // TODO:
      // gbCacheConfig.setMaxEntriesLocalHeap(config.getMaxCountLocalHeap() * 2 + 1);
    }

    if (config.getMaxBytesLocalHeap() > 0) {
      // TODO:
      // gbCacheConfig.setMaxBytesLocalHeap(config.getMaxBytesLocalHeap());
    }

    // TODO:
    // gbCacheConfig.setOverflowToOffHeap(config.isOverflowToOffheap());
    if (config.isOverflowToOffheap()) {
      long maxBytesLocalOffHeap = config.getMaxBytesLocalOffheap();
      if (maxBytesLocalOffHeap > 0) {
        // TODO:
        // gbCacheConfig.setMaxBytesLocalOffHeap(maxBytesLocalOffHeap);
      }
    }

    String pinningConfig = getPinningConfigString(config);
    if (pinningConfig != null && !pinningConfig.trim().equals("")) {
      // TODO:
      // gbCacheConfig.pinning(new PinningConfiguration().store(ehcachePinningConfig));
    }

    return gbCacheConfig;
  }

  private GBCacheConfig createConfig(String cacheName) {
    return null;
  }

  private static String getPinningConfigString(ServerMapLocalStoreConfig config) {
    PinningStore pinning = PinningStore.valueOf(config.getPinningStore());
    switch (pinning) {
      case INCACHE:
      case LOCALHEAP:
      case LOCALMEMORY:
        return pinning.name();
      case NONE:
        return "";
    }
    throw new UnsupportedOperationException();
  }
}