/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.internal.feature.ManagementInternalFeature;
import org.terracotta.toolkit.internal.feature.ToolkitManagementEvent;

import com.tc.management.TCManagementEvent;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.feature.EnabledToolkitFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class NonStopManagementInternalFeatureImpl extends EnabledToolkitFeature implements ManagementInternalFeature {

  private final List<ToolkitManagementEvent> bufferedEvents  = new ArrayList<ToolkitManagementEvent>();
  private volatile PlatformService           platformService = null;

  public synchronized void setPlatformService(PlatformService ps) {
    for (ToolkitManagementEvent event : bufferedEvents) {
      sendEvent(ps, event);
    }

    bufferedEvents.clear();
    
    platformService = ps;
  }

  @Override
  public Object registerManagementService(Object service, ExecutorService executorService) {
    PlatformService ps = platformService;
    if (ps == null) { throw new IllegalStateException(); }

    return ps.registerManagementService(service, executorService);
  }

  @Override
  public void unregisterManagementService(Object serviceID) {
    PlatformService ps = platformService;
    if (ps == null) { throw new IllegalStateException(); }

    ps.unregisterManagementService(serviceID);
  }

  @Override
  public void sendEvent(ToolkitManagementEvent event) {
    PlatformService ps = platformService;
    if (ps == null) {
      synchronized (this) {
        ps = platformService;
        if (ps == null) {
          bufferedEvents.add(event);
          return;
        }
      }
    }

    sendEvent(ps, event);
  }

  private static void sendEvent(PlatformService ps, ToolkitManagementEvent event) {
    ps.sendEvent(new TCManagementEvent(event.getPayload(), event.getType()));
  }

}
