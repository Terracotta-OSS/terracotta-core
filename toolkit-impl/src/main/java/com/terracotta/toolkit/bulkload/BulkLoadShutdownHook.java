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
package com.terracotta.toolkit.bulkload;

import com.tc.cluster.DsoCluster;
import com.tc.platform.PlatformService;

/**
 * @author Abhishek Sanoujam
 */
public class BulkLoadShutdownHook {
  private final DsoCluster                dsoCluster;
  private final PlatformService           platformService;
  private BulkLoadToolkitCache            registerBulkLoadToolkitCache;
  private final Runnable                  registorShutdown = new Runnable() {
                                                   @Override
                                                   public void run() {
                                                     shutdownRegisteredCache();
                                                   }
                                                 };
  public BulkLoadShutdownHook(PlatformService platformService) {
    this.platformService = platformService;
    dsoCluster = platformService.getDsoCluster();
  }

  private synchronized void shutdownRegisteredCache() {
    if (dsoCluster.areOperationsEnabled()) {
          if (registerBulkLoadToolkitCache.isNodeBulkLoadEnabled()) {
            registerBulkLoadToolkitCache.setNodeBulkLoadEnabled(false);
      }
    }
  }

  /**
   * Registers the cache for shutdown hook. When the node shuts down, if the cache is in BulkLoad mode, it will set the
   * cache back to coherent mode. Setting the node back to coherent mode will flush any changes that were made while in
   * incoherent mode.
   */
  public synchronized void registerCache(BulkLoadToolkitCache cache) {
    registerBulkLoadToolkitCache = cache;
    platformService.registerBeforeShutdownHook(registorShutdown);
  }
  /**
   * Unregisters the cache from shutdown hook.
   */
  public synchronized void unregisterCache(BulkLoadToolkitCache cache) {
    registerBulkLoadToolkitCache = null;
    platformService.unregisterBeforeShutdownHook(registorShutdown);
  }

}
