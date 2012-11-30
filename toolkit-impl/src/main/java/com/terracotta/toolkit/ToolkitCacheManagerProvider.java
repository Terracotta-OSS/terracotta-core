/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import net.sf.ehcache.CacheManager;

import java.util.UUID;

public class ToolkitCacheManagerProvider {
  private final CacheManager defaultToolkitCacheManager;

  public ToolkitCacheManagerProvider() {
    this.defaultToolkitCacheManager = createDefaultToolkitCacheManager();
  }

  private CacheManager createDefaultToolkitCacheManager() {
    String cacheManagerUniqueName = "toolkitDefaultCacheManager-" + UUID.randomUUID().toString();
    return CacheManager.newInstance(new net.sf.ehcache.config.Configuration().name(cacheManagerUniqueName));
  }

  public CacheManager getDefaultCacheManager() {
    return defaultToolkitCacheManager;
  }

}
