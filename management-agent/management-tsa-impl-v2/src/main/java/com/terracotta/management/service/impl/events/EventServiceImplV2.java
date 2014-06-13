/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.events;

import org.terracotta.management.resource.events.EventEntityV2;

import com.tc.management.ManagementEventListener;
import com.tc.management.RemoteManagement;
import com.tc.management.TCManagementEvent;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.terracotta.management.service.events.EventServiceV2;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class EventServiceImplV2 implements EventServiceV2 {

  private final Map<EventListener, ManagementEventListener> listenerMap = Collections.synchronizedMap(new IdentityHashMap<EventListener, ManagementEventListener>());

  @Override
  public void registerTopologyEventListener(final EventListener listener) {
    RemoteManagement remoteManagementInstance = TerracottaRemoteManagement.getRemoteManagementInstance();
    ManagementEventListener managementEventListener = new ManagementEventListener() {
      @Override
      public ClassLoader getClassLoader() {
        return EventServiceImplV2.class.getClassLoader();
      }

      @Override
      public void onEvent(TCManagementEvent event, Map<String, Object> context) {
//        if (!event.getType().startsWith("TSA.TOPOLOGY")) { return; }
        TSAManagementEventPayload tsaManagementEventPayload = (TSAManagementEventPayload)event.getPayload();

        EventEntityV2 topologyEventEntity = new EventEntityV2();
        topologyEventEntity.setSourceId((String)context.get(ManagementEventListener.CONTEXT_SOURCE_NODE_NAME));
        topologyEventEntity.setType(event.getType());
        topologyEventEntity.setTargetNodeId(tsaManagementEventPayload.getTargetNodeId());
        topologyEventEntity.setTargetJmxId(tsaManagementEventPayload.getTargetJmxId());

        listener.onEvent(topologyEventEntity);
      }
    };
    listenerMap.put(listener, managementEventListener);
    remoteManagementInstance.registerEventListener(managementEventListener);
  }

  @Override
  public void unregisterTopologyEventListener(EventListener listener) {
    RemoteManagement remoteManagementInstance = TerracottaRemoteManagement.getRemoteManagementInstance();
    remoteManagementInstance.unregisterEventListener(listenerMap.remove(listener));
  }
}
