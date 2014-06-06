/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.internal.feature.ManagementInternalFeature;
import org.terracotta.toolkit.internal.feature.ToolkitManagementEvent;

import com.tc.management.TCManagementEvent;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.feature.EnabledToolkitFeature;

import java.util.concurrent.ExecutorService;

public class ManagementInternalFeatureImpl extends EnabledToolkitFeature implements ManagementInternalFeature {

  private final PlatformService platformService;

  public ManagementInternalFeatureImpl(PlatformService platformService) {
    this.platformService = platformService;
  }

  @Override
  public Object registerManagementService(Object service, ExecutorService executorService) {
    return platformService.registerManagementService(service, executorService);
  }

  @Override
  public void unregisterManagementService(Object serviceID) {
    platformService.unregisterManagementService(serviceID);
  }

  @Override
  public void sendEvent(ToolkitManagementEvent event) {
    platformService.sendEvent(new TCManagementEvent(event.getPayload(), event.getType()));
  }

}
