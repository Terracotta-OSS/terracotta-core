/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
