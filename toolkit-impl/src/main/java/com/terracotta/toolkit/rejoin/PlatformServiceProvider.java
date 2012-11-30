/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.rejoin;

import com.tc.platform.PlatformService;
import com.tc.platform.StaticPlatformApi;

public class PlatformServiceProvider {
  private static final PlatformService platformService = createRejoinAwareIfNecessary();

  private static PlatformService createRejoinAwareIfNecessary() {
    PlatformService service = StaticPlatformApi.getPlatformService();
    if (service.isRejoinEnabled()) { return new RejoinAwarePlatformService(service); }
    return service;
  }

  public static PlatformService getPlatformService() {
    return platformService;
  }
}
