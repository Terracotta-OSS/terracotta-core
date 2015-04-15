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

  public void shutdownDefaultCacheManager() {
    defaultToolkitCacheManager.shutdown();
  }

}
