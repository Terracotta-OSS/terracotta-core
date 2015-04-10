/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.events;

import com.tc.management.ManagementEventListener;
import com.tc.management.RemoteManagement;
import com.tc.management.TCManagementEvent;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.terracotta.management.service.impl.util.RemoteManagementSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.terracotta.management.resource.Representable;
import org.terracotta.management.resource.events.EventEntityV2;
import org.terracotta.management.resource.services.events.EventServiceV2;

import java.io.Serializable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class EventServiceImplV2 implements EventServiceV2 {

  private final Map<EventListener, ListenerHolder> listenerMap = Collections.synchronizedMap(new IdentityHashMap<EventListener, ListenerHolder>());
  private final RemoteManagementSource remoteManagementSource;

  public EventServiceImplV2(RemoteManagementSource remoteManagementSource) {
    this.remoteManagementSource = remoteManagementSource;
  }

  @Override
  public void registerEventListener(final EventListener listener, boolean localOnly) {
    RemoteManagementSource.RemoteTSAEventListener remoteTSAEventListener = null;
    if (!localOnly) {
      remoteTSAEventListener = new RemoteManagementSource.RemoteTSAEventListener() {
        @Override
        public void onEvent(InboundEvent inboundEvent) {
          EventEntityV2 eventEntity = inboundEvent.readData(EventEntityV2.class);
          listener.onEvent(eventEntity);
        }

        @Override
        public void onError(Throwable throwable) {
          listener.onError(throwable);
        }
      };
      remoteManagementSource.addTsaEventListener(remoteTSAEventListener);
    }

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
          String remoteAddress = (String) context.get(ManagementEventListener.CONTEXT_SOURCE_JMX_ID);
          String clientID = (String) context.get(ManagementEventListener.CONTEXT_SOURCE_NODE_NAME);
          eventEntity.getRootRepresentables().put("RemoteAddress", remoteAddress);
          eventEntity.getRootRepresentables().put("ClientID", clientID);
          eventEntity.setAgentId(remoteAddress.replace(':', '_'));
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
    listenerMap.put(listener, new ListenerHolder(managementEventListener, remoteTSAEventListener));
    remoteManagementInstance.registerEventListener(managementEventListener);
  }

  @Override
  public void unregisterEventListener(EventListener listener) {
    ListenerHolder listenerHolder = listenerMap.remove(listener);
    if (listenerHolder != null) {
      RemoteManagement remoteManagementInstance = TerracottaRemoteManagement.getRemoteManagementInstance();
      remoteManagementInstance.unregisterEventListener(listenerHolder.managementEventListener);
      if (listenerHolder.remoteTSAEventListener != null) {
        remoteManagementSource.removeTsaEventListener(listenerHolder.remoteTSAEventListener);
      }
    }
  }

  static class ListenerHolder {
    ManagementEventListener managementEventListener;
    RemoteManagementSource.RemoteTSAEventListener remoteTSAEventListener;

    ListenerHolder(ManagementEventListener managementEventListener, RemoteManagementSource.RemoteTSAEventListener remoteTSAEventListener) {
      this.managementEventListener = managementEventListener;
      this.remoteTSAEventListener = remoteTSAEventListener;
    }
  }

}
