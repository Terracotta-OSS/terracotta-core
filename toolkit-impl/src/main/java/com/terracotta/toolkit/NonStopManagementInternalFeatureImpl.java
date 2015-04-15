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
