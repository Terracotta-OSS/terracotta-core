/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.async.api.EventContext;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tcclient.cluster.DsoClusterInternal.DsoClusterEventType;

/**
 * DsoCluster Events Contexts to be put in ClusterInternalEventsHandler.
 */
public class ClusterInternalEventsContext implements EventContext {

  private final DsoClusterEventType eventType;
  private final DsoClusterEvent     event;
  private final DsoClusterListener  dsoClusterListener;

  public ClusterInternalEventsContext(DsoClusterEventType eventType, DsoClusterEvent event, DsoClusterListener listener) {
    this.eventType = eventType;
    this.event = event;
    this.dsoClusterListener = listener;
  }

  public DsoClusterEventType getEventType() {
    return eventType;
  }

  public DsoClusterEvent getEvent() {
    return event;
  }

  public DsoClusterListener getDsoClusterListener() {
    return dsoClusterListener;
  }

  @Override
  public String toString() {
    return "ClusterInternalEventsContext [eventType=" + eventType + ", event=" + event + ", dsoClusterListener="
           + dsoClusterListener + "]";
  }

}
