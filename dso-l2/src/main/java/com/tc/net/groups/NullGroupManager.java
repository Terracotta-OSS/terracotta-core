/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.config.GroupConfiguration;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
= */
public class NullGroupManager implements GroupManager<AbstractGroupMessage> {
  private final ServerID thisNode;

  public NullGroupManager(ServerID thisNode) {
    this.thisNode = thisNode;
  }
  
  @Override
  public NodeID join(GroupConfiguration groupConfiguration) throws GroupException {
    return thisNode;
  }

  @Override
  public NodeID getLocalNodeID() {
    return thisNode;
  }

  @Override
  public void sendAll(AbstractGroupMessage msg) {

  }

  @Override
  public void sendAll(AbstractGroupMessage msg, Set<? extends NodeID> nodeIDs) {

  }

  @Override
  public GroupResponse<AbstractGroupMessage> sendAllAndWaitForResponse(AbstractGroupMessage msg) throws GroupException {
    return new GroupResponse<AbstractGroupMessage>() {
      @Override
      public List<AbstractGroupMessage> getResponses() {
        return Collections.emptyList();
      }

      @Override
      public AbstractGroupMessage getResponse(NodeID nodeID) {
        return null;
      }
    };
  }

  @Override
  public GroupResponse<AbstractGroupMessage> sendAllAndWaitForResponse(AbstractGroupMessage msg, Set<? extends NodeID> nodeIDs) throws GroupException {
    return new GroupResponse<AbstractGroupMessage>() {
      @Override
      public List<AbstractGroupMessage> getResponses() {
        return Collections.emptyList();
      }

      @Override
      public AbstractGroupMessage getResponse(NodeID nodeID) {
        return null;
      }
    };
  }

  @Override
  public void sendTo(NodeID node, AbstractGroupMessage msg) throws GroupException {

  }

  @Override
  public void sendToWithSentCallback(NodeID node, AbstractGroupMessage msg, Runnable sentCallback) throws GroupException {
    sentCallback.run();
  }

  @Override
  public AbstractGroupMessage sendToAndWaitForResponse(NodeID nodeID, AbstractGroupMessage msg) throws GroupException {
    throw new UnsupportedOperationException(); 
  }

  @Override
  public <N extends AbstractGroupMessage> void registerForMessages(Class<? extends N> msgClass, GroupMessageListener<N> listener) {

  }

  @Override
  public <N extends AbstractGroupMessage> void routeMessages(Class<? extends N> msgClass, Sink<N> sink) {

  }

  @Override
  public void registerForGroupEvents(GroupEventsListener listener) {

  }

  @Override
  public void zapNode(NodeID nodeID, int type, String reason) {

  }

  @Override
  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {

  }

  @Override
  public boolean isNodeConnected(NodeID sid) {
    return thisNode.equals(sid);
  }

  @Override
  public boolean isServerConnected(String nodeName) {
    return thisNode.getName().equals(nodeName);
  }

  @Override
  public void closeMember(ServerID serverID) {

  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", this.getClass());
    map.put("description", "no known peer servers");
    return map;
  }
  
}
