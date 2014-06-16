/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.events;

import org.terracotta.management.resource.Representable;
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
  public void registerEventListener(final EventListener listener) {
    RemoteManagement remoteManagementInstance = TerracottaRemoteManagement.getRemoteManagementInstance();
    ManagementEventListener managementEventListener = new ManagementEventListener() {
      @Override
      public ClassLoader getClassLoader() {
        return EventServiceImplV2.class.getClassLoader();
      }

      @Override
      public void onEvent(TCManagementEvent event, Map<String, Object> context) {
        TSAManagementEventPayload tsaManagementEventPayload = (TSAManagementEventPayload)event.getPayload();

        EventEntityV2 eventEntity = new EventEntityV2();
        if (event.getType().startsWith("TSA")) {
          eventEntity.setAgentId(Representable.EMBEDDED_AGENT_ID);
        } else {
          eventEntity.setAgentId(tsaManagementEventPayload.getTargetJmxId());
        }
        eventEntity.getRootRepresentables().put("source.server.name", context.get(ManagementEventListener.CONTEXT_SOURCE_NODE_NAME));
        eventEntity.getRootRepresentables().put("source.client.nodeId", tsaManagementEventPayload.getTargetNodeId());
        eventEntity.setType(event.getType());

        listener.onEvent(eventEntity);
      }
    };
    listenerMap.put(listener, managementEventListener);
    remoteManagementInstance.registerEventListener(managementEventListener);
  }

  @Override
  public void unregisterEventListener(EventListener listener) {
    RemoteManagement remoteManagementInstance = TerracottaRemoteManagement.getRemoteManagementInstance();
    remoteManagementInstance.unregisterEventListener(listenerMap.remove(listener));
  }
}
