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
    return event.getKey();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ServerEventDeliveryContext that = (ServerEventDeliveryContext) o;

    if (!event.equals(that.event)) return false;
    if (!remoteNode.equals(that.remoteNode)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = event.hashCode();
    result = 31 * result + remoteNode.hashCode();
    return result;
  }
}
