/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.offheap;

import net.sf.ehcache.CacheManager;

import org.junit.Assert;
import org.junit.Test;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreConfig;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreConfigParameters;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFullException;
import com.terracotta.toolkit.collections.servermap.api.ehcacheimpl.EhcacheSMLocalStoreFactory;

public class OffheapKeyLookupCheckTest {

  @Test
  public void testKeyLookupLocalCache() throws ServerMapLocalStoreFullException {
    EhcacheSMLocalStoreFactory localCacheFactory = new EhcacheSMLocalStoreFactory(new CacheManager());
    ServerMapLocalStoreConfigParameters configParameters = new ServerMapLocalStoreConfigParameters();
    configParameters.overflowToOffheap(false);
    configParameters.localStoreName("test-store");
    ServerMapLocalStoreConfig localStoreConfig = new ServerMapLocalStoreConfig(configParameters);
    ServerMapLocalStore<Object, Object> localStore = localCacheFactory.getOrCreateServerMapLocalStore(localStoreConfig);
    localStore.put("name", "Vikas");
    Assert.assertFalse(localStore.containsKeyOffHeap("name"));
    Assert.assertTrue(localStore.containsKeyOnHeap("name"));
  }

}
