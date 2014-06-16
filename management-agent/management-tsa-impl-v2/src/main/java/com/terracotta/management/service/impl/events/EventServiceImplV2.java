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

import java.io.Serializable;
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
        Serializable payload = event.getPayload();
        EventEntityV2 eventEntity;
        if (payload instanceof EventEntityV2) {
          eventEntity = (EventEntityV2)payload;
          eventEntity.setAgentId((String)context.get(ManagementEventListener.CONTEXT_SOURCE_JMX_ID));
        } else if (payload instanceof TSAManagementEventPayload) {
          TSAManagementEventPayload tsaManagementEventPayload = (TSAManagementEventPayload)payload;

          eventEntity = new EventEntityV2();
          eventEntity.setAgentId(Representable.EMBEDDED_AGENT_ID);
          eventEntity.getRootRepresentables().put("Server.Name", context.get(ManagementEventListener.CONTEXT_SOURCE_NODE_NAME));
          eventEntity.getRootRepresentables().putAll(tsaManagementEventPayload.getAttributes());
          eventEntity.setType(event.getType());
        } else {
          eventEntity = new EventEntityV2();
          eventEntity.setType("TSA.ERROR");
          eventEntity.setAgentId(Representable.EMBEDDED_AGENT_ID);
          eventEntity.getRootRepresentables().put("Error.Details", "Unknown event : " + payload);
        }
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
