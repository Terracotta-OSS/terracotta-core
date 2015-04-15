/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
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
