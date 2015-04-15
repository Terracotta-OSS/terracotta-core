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
