/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache;

import java.lang.reflect.Method;

/**
 * Helper class used for initializing ehcache's using protected {@link CacheManager#initializeEhcache(Ehcache, boolean)}
 * method
 */
public class EhcacheInitializationHelper {
  private final CacheManager cacheManager;

  /**
   * Create a cache initializer with the given {@link CacheManager}
   * 
   * @param cacheManager
   */
  public EhcacheInitializationHelper(final CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /**
   * Initialize the {@link Ehcache}.
   * 
   * @param cache
   */
  public void initializeEhcache(final Ehcache cache) {
    // CacheManager can be loaded using clusteredStateLoader in case of embedded ehcache or AppClassLoader if
    // ehcache-core is present in classpath. Reflection is used here to handle both the cases.
    try {
      Method method = this.cacheManager.getClass().getDeclaredMethod("initializeEhcache", Ehcache.class, boolean.class);
      method.setAccessible(true);
      method.invoke(cacheManager, cache, false);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
