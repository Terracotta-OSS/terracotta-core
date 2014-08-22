/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.events;

import com.terracotta.management.service.L1AgentIdRetrievalServiceV2;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.Representable;
import org.terracotta.management.resource.events.EventEntityV2;
import org.terracotta.management.resource.services.events.EventServiceV2;

import com.tc.management.ManagementEventListener;
import com.tc.management.RemoteManagement;
import com.tc.management.TCManagementEvent;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.terracotta.management.service.impl.util.RemoteManagementSource;

import java.io.Serializable;
import java.util.*;

/**
 * @author Ludovic Orban
 */
public class EventServiceImplV2 implements EventServiceV2 {

  private static final Logger LOG = LoggerFactory.getLogger(EventServiceImplV2.class);
  private final Map<EventListener, ListenerHolder> listenerMap = Collections.synchronizedMap(new IdentityHashMap<EventListener, ListenerHolder>());
  private final RemoteManagementSource remoteManagementSource;
  private final L1AgentIdRetrievalServiceV2 l1AgentIdRetrievalService;

  public EventServiceImplV2(RemoteManagementSource remoteManagementSource, L1AgentIdRetrievalServiceV2 l1AgentIdRetrievalService) {
    this.remoteManagementSource = remoteManagementSource;
    this.l1AgentIdRetrievalService = l1AgentIdRetrievalService;
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
          eventEntity.getRootRepresentables().put("RemoteAddress", remoteAddress);
          try {
            eventEntity.setAgentId(l1AgentIdRetrievalService.getAgentIdFromRemoteAddress(remoteAddress));
          } catch (ServiceExecutionException e) {
            LOG.warn("Could not retrieve agentId for remoteAddress " + remoteAddress,e);
          }
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
