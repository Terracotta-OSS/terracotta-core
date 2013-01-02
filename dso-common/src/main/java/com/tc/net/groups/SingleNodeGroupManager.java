/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.config.NodesStore;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.text.PrettyPrinter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * This is a simple class that is a dummy group manager. All it does is it treats this one node that it runs in as a
 * group. This is needed for two reasons, 1) to easily disable, test the rest of the system and 2) to provide an
 * interface level replacement in disk based active passive where group manager is not needed.
 */
public class SingleNodeGroupManager implements GroupManager {

  private static final GroupResponse DUMMY_RESPONSE  = new GroupResponse() {
                                                       @Override
                                                      public List getResponses() {
                                                         return Collections.EMPTY_LIST;
                                                       }

                                                       @Override
                                                      public GroupMessage getResponse(NodeID nodeID) {
                                                         return null;
                                                       }
                                                     };

  private static final byte[]        CURRENT_NODE_ID = new byte[] { 36, 24, 32 };

  private final AtomicBoolean        joined          = new AtomicBoolean(false);
  private final NodeID               thisNode;

  public SingleNodeGroupManager(NodeID localNodeID) {
    thisNode = localNodeID;
  }

  public SingleNodeGroupManager() {
    this(new ServerID("CurrentNode", CURRENT_NODE_ID));
  }

  @Override
  public NodeID join(final Node thisN, final NodesStore nodesStore) throws GroupException {
    if (!joined.compareAndSet(false, true)) { throw new GroupException("Already Joined"); }

    return this.thisNode;
  }

  @Override
  public NodeID getLocalNodeID() {
    return this.thisNode;
  }

  @Override
  public void registerForMessages(Class msgClass, GroupMessageListener listener) {
    // NOP : Since this doesn't talk to the network, this should never get any message
  }

  @Override
  public void routeMessages(Class msgClass, Sink sink) {
    // NOP : Since this doesn't talk to the network, this should never get any message
  }

  @Override
  public void sendAll(GroupMessage msg) {
    // NOP : No Network, no one to write to
  }

  @Override
  public void sendAll(GroupMessage msg, Set nodeIDs) {
    // NOP
  }

  @Override
  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) {
    // NOP : No Network, no one to write to, hen no response too
    return DUMMY_RESPONSE;
  }

  @Override
  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg, Set nodeIDs) {
    // NOP : No Network, no one to write to, hen no response too
    return DUMMY_RESPONSE;
  }

  @Override
  public void sendTo(NodeID node, GroupMessage msg) throws GroupException {
    throw new GroupException("Can't write to Node : " + node + " Node Not found !");
  }

  @Override
  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) {
    // Come back
    return null;
  }

  @Override
  public void registerForGroupEvents(GroupEventsListener listener) {
    // NOP : No network, no one joins or leaves
  }

  @Override
  public void zapNode(NodeID nodeID, int type, String reason) {
    // what node ?
  }

  @Override
  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    // NOP
  }

  @Override
  public boolean isNodeConnected(NodeID sid) {
    return true;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out;
  }

  @Override
  public boolean isServerConnected(String nodeName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void closeMember(ServerID serverID) {
    // NOP
  }
}
