/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;
import com.tcclient.cluster.DsoClusterInternal.EVENTS;

import java.util.Arrays;

/**
 * DsoCluster Events Contexts to be put in ClusterInternalEventsHandler.
 */
public class ClusterInternalEventsContext implements EventContext {

  private final EVENTS   eventType;
  private final NodeID   eventNodeID;
  private final NodeID[] otherNodeIDs;

  public ClusterInternalEventsContext(final EVENTS eventType) {
    this(eventType, null);
  }

  public ClusterInternalEventsContext(final EVENTS eventType, final NodeID eventNodeID) {
    this(eventType, eventNodeID, null);
  }

  public ClusterInternalEventsContext(final EVENTS eventType, final NodeID eventNodeID, final NodeID[] otherNodeIDs) {
    this.eventType = eventType;
    this.eventNodeID = eventNodeID;
    this.otherNodeIDs = otherNodeIDs;
  }

  public EVENTS getEventType() {
    return this.eventType;
  }

  public NodeID getEventNodeID() {
    return eventNodeID;
  }

  public NodeID[] getOtherNodeIDs() {
    return otherNodeIDs;
  }

  @Override
  public String toString() {
    return "ClusterInternalEventsMessage [EventType: " + this.eventType + "; EventNodeID: " + this.eventNodeID
           + "; OtherNodeIDs: " + Arrays.toString(this.otherNodeIDs) + "]";
  }
}
