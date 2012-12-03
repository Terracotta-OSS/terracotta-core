/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.rejoin;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.platform.PlatformService;

public class PlatformServiceProvider {
  private static final PlatformService platformService = createRejoinAwareIfNecessary();

  private static PlatformService createRejoinAwareIfNecessary() {
    PlatformService service = ManagerUtil.getManager().getPlatformService();
    if (service.isRejoinEnabled()) { return new RejoinAwarePlatformService(service); }
    return service;
  }

  public static PlatformService getPlatformService() {
    return platformService;
  }
}
