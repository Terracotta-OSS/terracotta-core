/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
