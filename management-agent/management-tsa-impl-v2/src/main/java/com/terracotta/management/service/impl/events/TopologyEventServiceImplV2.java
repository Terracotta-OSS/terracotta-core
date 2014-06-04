/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.events;

import com.tc.management.ManagementEventListener;
import com.tc.management.RemoteManagement;
import com.tc.management.TSAManagementEvent;
import com.tc.management.TerracottaRemoteManagement;
import com.terracotta.management.resource.events.TopologyEventEntityV2;
import com.terracotta.management.service.events.TopologyEventServiceV2;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class TopologyEventServiceImplV2 implements TopologyEventServiceV2 {

  @Override
  public void registerTopologyEventListener(final TopologyEventListener listener) {
    RemoteManagement remoteManagementInstance = TerracottaRemoteManagement.getRemoteManagementInstance();
    remoteManagementInstance.registerEventListener(new ManagementEventListener() {
      @Override
      public ClassLoader getClassLoader() {
        return TopologyEventServiceImplV2.class.getClassLoader();
      }

      @Override
      public void onEvent(Serializable event, Map<String, Object> context) {
        TSAManagementEvent tsaManagementEvent = (TSAManagementEvent)event;

        TopologyEventEntityV2 topologyEventEntity = new TopologyEventEntityV2();
        topologyEventEntity.setSourceId((String)context.get(ManagementEventListener.CONTEXT_SOURCE_NODE_NAME));
        topologyEventEntity.setEvent(tsaManagementEvent.getType());
        topologyEventEntity.setTargetId(tsaManagementEvent.getTargetNodeId());

        listener.onEvent(topologyEventEntity);
      }
    });
  }

  @Override
  public void unregisterTopologyEventListener(TopologyEventListener listener) {

  }
}
