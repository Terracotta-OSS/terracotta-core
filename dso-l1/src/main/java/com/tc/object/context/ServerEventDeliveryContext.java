package com.tc.object.context;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.NodeID;
import com.tc.server.ServerEvent;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventDeliveryContext implements MultiThreadedEventContext {

  private final ServerEvent event;
  private final NodeID remoteNode;

  public ServerEventDeliveryContext(final ServerEvent event, final NodeID remoteNode) {
    this.event = event;
    this.remoteNode = remoteNode;
  }

  public ServerEvent getEvent() {
    return event;
  }

  public NodeID getRemoteNode() {
    return remoteNode;
  }

  @Override
  public Object getKey() {
    return event.getKey().hashCode();
  }
}
