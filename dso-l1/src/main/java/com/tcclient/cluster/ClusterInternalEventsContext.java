/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.cluster.ClusterEvent;
import com.tc.cluster.ClusterListener;
import com.tcclient.cluster.ClusterInternal.ClusterEventType;

/**
 * Cluster Events Contexts to be put in ClusterInternalEventsHandler.
 */
public class ClusterInternalEventsContext {

  private final ClusterEventType eventType;
  private final ClusterEvent     event;
  private final ClusterListener clusterListener;

  public ClusterInternalEventsContext(ClusterEventType eventType, ClusterEvent event, ClusterListener listener) {
    this.eventType = eventType;
    this.event = event;
    this.clusterListener = listener;
  }

  public ClusterEventType getEventType() {
    return eventType;
  }

  public ClusterEvent getEvent() {
    return event;
  }

  public ClusterListener getClusterListener() {
    return clusterListener;
  }

  @Override
  public String toString() {
    return "ClusterInternalEventsContext [eventType=" + eventType + ", event=" + event + ", clusterListener=" + clusterListener + "]";
  }

}
