package com.tc.net.groups;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.MembershipListener;
import org.apache.catalina.tribes.ChannelException.FaultyMember;
import org.apache.catalina.tribes.group.ChannelCoordinator;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.OrderInterceptor;
import org.apache.catalina.tribes.group.interceptors.StaticMembershipInterceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.group.interceptors.TcpPingInterceptor;
import org.apache.catalina.tribes.membership.StaticMember;
import org.apache.catalina.tribes.transport.DataSender;
import org.apache.catalina.tribes.transport.ReceiverBase;
import org.apache.catalina.tribes.transport.ReplicationTransmitter;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.concurrent.CopyOnWriteArrayMap;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TribesGroupManager implements GroupManager, ChannelListener, MembershipListener {

  private static final String                             L2_NHA                  = "l2.nha";
  private static final String                             SEND_TIMEOUT_PROP       = "send.timeout.millis";
  private static final String                             USE_MCAST               = "mcast.enabled";
  private static final int                                SEND_OPTIONS_NO_ACK     = 0x00;

  private static final TCLogger                           logger                  = TCLogging
                                                                                      .getLogger(TribesGroupManager.class);

  private static final boolean                            useMcast                = TCPropertiesImpl.getProperties()
                                                                                      .getPropertiesFor(L2_NHA)
                                                                                      .getBoolean(USE_MCAST);

  private final GroupChannel                              group;
  private TcpFailureDetector                              failuredetector;
  private Member                                          thisMember;
  private NodeID                                          thisNodeID;

  private final CopyOnWriteArrayList<GroupEventsListener> groupListeners          = new CopyOnWriteArrayList<GroupEventsListener>();
  // private final Map<NodeID, Member> nodes = new CopyOnWriteArrayMap<NodeID, Member>();
  private final CopyOnWriteArrayMap                       nodes                   = new CopyOnWriteArrayMap(
                                                                                                            new CopyOnWriteArrayMap.TypedArrayFactory() {
                                                                                                              public Object[] createTypedArray(
                                                                                                                                               int size) {
                                                                                                                return new Member[size];
                                                                                                              }
                                                                                                            });
  private final Map<String, GroupMessageListener>         messageListeners        = new ConcurrentHashMap<String, GroupMessageListener>();
  private final Map<MessageID, GroupResponse>             pendingRequests         = new Hashtable<MessageID, GroupResponse>();

  private boolean                                         stopped                 = false;
  private boolean                                         debug                   = false;
  private ZapNodeRequestProcessor                         zapNodeRequestProcessor = new DefaultZapNodeRequestProcessor(
                                                                                                                       logger);

  public TribesGroupManager() {
    group = new GroupChannel();
    registerForMessages(GroupZapNodeMessage.class, new ZapNodeRequestRouter());
  }

  public NodeID join(final Node thisNode, final Node[] allNodes) throws GroupException {
    if (useMcast) return joinMcast();
    else return joinStatic(thisNode, allNodes);

  }

  public synchronized void stop() throws GroupException {
    try {
      group.stop(Channel.DEFAULT);
    } catch (ChannelException e) {
      logger.error(e);
      throw new GroupException(e);
    } finally {
      stopped = true;
    }
  }

  private void commonGroupChanelConfig() {
    // config send timeout
    ReplicationTransmitter transmitter = (ReplicationTransmitter) group.getChannelSender();
    DataSender sender = transmitter.getTransport();
    final long l = TCPropertiesImpl.getProperties().getPropertiesFor(L2_NHA).getLong(SEND_TIMEOUT_PROP);
    sender.setTimeout(l);
    ChannelCoordinator cc = (ChannelCoordinator) group.getNext();
    final Properties mcastProps = new Properties();
    TCPropertiesImpl.getProperties().getPropertiesFor("l2.nha.tribes.mcast").addAllPropertiesTo(mcastProps);
    cc.getMembershipService().setProperties(mcastProps);
    // add listeners
    group.addMembershipListener(this);
    group.addChannelListener(this);

  }

  protected NodeID joinStatic(final Node thisNode, final Node[] allNodes) throws GroupException {
    try {
      // set up static nodes
      StaticMembershipInterceptor smi = setupStaticMembers(thisNode, allNodes);

      // set up receiver
      ReceiverBase receiver = (ReceiverBase) group.getChannelReceiver();
      receiver.setAddress(thisNode.getHost());
      receiver.setPort(thisNode.getPort());
      receiver.setAutoBind(0);

      commonGroupChanelConfig();
      TcpPingInterceptor tcp = new TcpPingInterceptor();
      tcp.setUseThread(true);
      tcp.setInterval(1000);

      OrderInterceptor oi = new OrderInterceptor();
      oi.setExpire(60000);

      // start services
      // set up failure detector
      failuredetector = new TcpFailureDetector();
      group.addInterceptor(oi);
      group.addInterceptor(tcp);
      group.addInterceptor(failuredetector);
      group.addInterceptor(smi);
      group.start(Channel.SND_RX_SEQ | Channel.SND_TX_SEQ);
      return this.thisNodeID;
    } catch (ChannelException e) {
      logger.error(e);
      throw new GroupException(e);
    }
  }

  protected NodeID joinMcast() throws GroupException {
    try {
      commonGroupChanelConfig();
      OrderInterceptor oi = new OrderInterceptor();
      oi.setExpire(60000);

      group.addInterceptor(oi);
      group.start(Channel.DEFAULT);
      this.thisMember = group.getLocalMember(false);
      this.thisNodeID = makeNodeIDFrom(this.thisMember);
      return this.thisNodeID;
    } catch (ChannelException e) {
      logger.error(e);
      throw new GroupException(e);
    }
  }

  /**
   * XXX:: This method is a temperary hack to make TribesGroupManager work. Without this for static members, we get
   * differernt UniqueID for the same members, one in nodeJoined event and one in the messages. Until that is fixed, the
   * NodeIDs uid is going to be based on the host and port for static members.
   */
  private static NodeID makeNodeIDFrom(Member member) {
    if (useMcast) {
      return new NodeID(member.getName(), member.getUniqueId());
    } else {
      byte[] host = member.getHost();
      int port = member.getPort();
      if (port < 0) {
        port = member.getSecurePort();
        if (port < 0) {
          // Ports shouldnt be 0 either, but in our test framework when there is only one inprocess active, it could be.
          throw new AssertionError("Invalid port number : " + port + " for host " + Conversion.bytesToHex(host));
        }
      }
      int length = host.length;
      byte uid[] = new byte[length + 4];
      System.arraycopy(host, 0, uid, 0, length);
      Conversion.writeInt(port, uid, length);
      return new NodeID(member.getName(), uid);
    }
  }

  private StaticMembershipInterceptor setupStaticMembers(final Node thisNode, final Node[] allNodes)
      throws AssertionError {
    StaticMembershipInterceptor smi = new StaticMembershipInterceptor();
    for (int i = 0; i < allNodes.length; i++) {
      final Node node = allNodes[i];
      if (thisNode.equals(node)) continue;
      StaticMember sm = makeMember(node);
      if (sm == null) continue;
      smi.addStaticMember(sm);
    }
    // set up this node
    thisMember = makeMember(thisNode);
    this.thisNodeID = makeNodeIDFrom(thisMember);
    if (thisMember == null) { throw new AssertionError("Error setting up this group member: " + thisNode); }
    smi.setLocalMember(thisMember);
    return smi;
  }

  public NodeID getLocalNodeID() throws GroupException {
    if (this.thisNodeID == null) { throw new GroupException("Node hasnt joined the group yet !"); }
    return this.thisNodeID;
  }

  private static void validateExternalizableClass(Class clazz) {
    String name = clazz.getName();
    try {
      Constructor cons = clazz.getDeclaredConstructor(new Class[0]);
      if ((cons.getModifiers() & Modifier.PUBLIC) == 0) {
        //
        throw new AssertionError(name + " : public no arg constructor not found");
      }
    } catch (NoSuchMethodException ex) {
      throw new AssertionError(name + " : public no arg constructor not found");
    }
  }

  private static void validateEventClass(Class clazz) {
    if (!EventContext.class.isAssignableFrom(clazz)) { throw new AssertionError(clazz
                                                                                + " does not implement interface "
                                                                                + EventContext.class.getName()); }
  }

  public void registerForMessages(Class msgClass, GroupMessageListener listener) {
    validateExternalizableClass(msgClass);
    GroupMessageListener prev = messageListeners.put(msgClass.getName(), listener);
    if (prev != null) {
      logger.warn("Previous listener removed : " + prev);
    }
  }

  public void routeMessages(Class msgClass, Sink sink) {
    validateEventClass(msgClass);
    registerForMessages(msgClass, new RouteGroupMessagesToSink(msgClass.getName(), sink));
  }

  public boolean accept(Serializable msg, Member sender) {
    if (stopped) return false;
    if (msg instanceof GroupMessage) { return true; }
    logger.warn("Rejecting unknown message : " + msg + " from " + sender.getName());
    return false;
  }

  public void messageReceived(Serializable msg, Member sender) {
    GroupMessage gmsg = (GroupMessage) msg;
    if (debug) {
      logger.info(this.thisNodeID + " recd msg " + gmsg.getMessageID() + " From " + sender.getName() + " Msg : " + msg);
    }
    MessageID requestID = gmsg.inResponseTo();
    NodeID from = makeNodeIDFrom(sender);
    if (!nodes.containsKey(from)) {
      String warn = "Message from non-existing member " + sender + " . Adding this node to nodes = " + nodes;
      logger.warn(warn);
      // XXX:: Sometimes messages arrive before memberAdded event. So we are faking it.
      memberAdded(sender);
    }
    gmsg.setMessageOrginator(from);
    if (requestID.isNull() || !notifyPendingRequests(requestID, gmsg, sender)) {
      fireMessageReceivedEvent(from, gmsg);
    }
  }

  private static StaticMember makeMember(final Node node) {
    try {
      StaticMember rv = new StaticMember(node.getHost(), node.getPort(), 0);
      // rv.setUniqueId(UUIDGenerator.randomUUID(true));
      return rv;
    } catch (IOException e) {
      logger.error("Error creating group member", e);
      return null;
    }
  }

  private boolean notifyPendingRequests(MessageID requestID, GroupMessage gmsg, Member sender) {
    GroupResponseImpl response = (GroupResponseImpl) pendingRequests.get(requestID);
    if (response != null) {
      response.addResponseFrom(sender, gmsg);
      return true;
    }
    return false;
  }

  private void fireMessageReceivedEvent(NodeID from, GroupMessage msg) {
    GroupMessageListener listener = messageListeners.get(msg.getClass().getName());
    if (listener != null) {
      listener.messageReceived(from, msg);
    } else {
      String errorMsg = "No Route for " + msg + " from " + from;
      logger.error(errorMsg);
      throw new AssertionError(errorMsg);
    }

  }

  public void registerForGroupEvents(GroupEventsListener listener) {
    groupListeners.add(listener);
  }

  public void memberAdded(Member member) {
    if (debug) {
      logger.info("memberAdded -> name=" + member.getName() + ", uid=" + Conversion.bytesToHex(member.getUniqueId()));
    }
    NodeID newNode = makeNodeIDFrom(member);
    Member old;
    if ((old = (Member) nodes.put(newNode, member)) == null) {
      fireNodeEvent(newNode, true);
    } else {
      logger.warn("Member Added Event called for : " + newNode + " while it is still present in the list of nodes : "
                  + old + " : " + nodes);
      if (!old.equals(member)) {
        logger.error("Old Member : " + old + " NOT Equal to  New one " + member);
      }
    }
  }

  private void fireNodeEvent(NodeID newNode, boolean joined) {
    if (debug) {
      logger.info("fireNodeEvent: joined=" + joined + ", name=" + newNode.getName() + ", uid="
                  + Conversion.bytesToHex(newNode.getUID()));
    }

    Iterator i = groupListeners.iterator();
    while (i.hasNext()) {
      GroupEventsListener listener = (GroupEventsListener) i.next();
      if (joined) {
        listener.nodeJoined(newNode);
      } else {
        listener.nodeLeft(newNode);
      }
    }
  }

  public void memberDisappeared(Member member) {
    if (debug) {
      logger.info("memberDisappeared -> name=" + member.getName() + ", uid="
                  + Conversion.bytesToHex(member.getUniqueId()));
    }

    NodeID node = makeNodeIDFrom(member);
    if ((nodes.remove(node)) != null) {
      fireNodeEvent(node, false);
    } else {
      logger.warn("Member Disappered Event called for : " + node + " while it is not present in the list of nodes : "
                  + nodes);
    }
    notifyAnyPendingRequests(member);
  }

  private void notifyAnyPendingRequests(Member member) {
    synchronized (pendingRequests) {
      for (Iterator i = pendingRequests.values().iterator(); i.hasNext();) {
        GroupResponseImpl response = (GroupResponseImpl) i.next();
        response.notifyMemberDead(member);
      }
    }
  }

  public void sendAll(GroupMessage msg) throws GroupException {
    if (debug) {
      logger.info(this.thisNodeID + " : Sending to ALL : " + msg.getMessageID());
    }
    try {
      Member m[] = getCurrentMembers();
      if (m.length > 0) {
        group.send(m, msg, SEND_OPTIONS_NO_ACK);
      }
    } catch (ChannelException e) {
      throw new GroupException(e);
    }
  }

  private Member[] getCurrentMembers() {
    // return group.getMembers();
    return (Member[]) nodes.valuesToArray();
  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) throws GroupException {
    if (debug) {
      logger.info(this.thisNodeID + " : Sending to ALL and Waiting for Response : " + msg.getMessageID());
    }
    GroupResponseImpl groupResponse = new GroupResponseImpl();
    MessageID msgID = msg.getMessageID();
    GroupResponse old = pendingRequests.put(msgID, groupResponse);
    Assert.assertNull(old);
    groupResponse.sendTo(group, msg, getCurrentMembers());
    groupResponse.waitForAllResponses();
    pendingRequests.remove(msgID);
    return groupResponse;
  }

  public void sendTo(NodeID node, GroupMessage msg) throws GroupException {
    if (debug) {
      logger.info(this.thisNodeID + " : Sending to : " + node + " msg " + msg.getMessageID() + " node.name="
                  + node.getName() + ", node.uid=" + Conversion.bytesToHex(node.getUID()));
    }
    try {
      Member member = (Member) nodes.get(node);
      if (member != null) {
        group.send(new Member[] { member }, msg, SEND_OPTIONS_NO_ACK);
      } else {
        // TODO:: These could be exceptions
        logger.warn("Ignoring Msg sent to: Node " + node + " not present in the group. Msg : " + msg);
      }
    } catch (ChannelException e) {
      throw new GroupException(e);
    }
  }

  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) throws GroupException {
    if (debug) {
      logger.info(this.thisNodeID + " : Sending to " + nodeID + " and Waiting for Response : " + msg.getMessageID());
    }
    GroupResponseImpl groupResponse = new GroupResponseImpl();
    MessageID msgID = msg.getMessageID();
    Member to[] = new Member[1];
    to[0] = (Member) nodes.get(nodeID);
    if (to[0] != null) {
      GroupResponse old = pendingRequests.put(msgID, groupResponse);
      Assert.assertNull(old);
      groupResponse.sendTo(group, msg, to);
      groupResponse.waitForAllResponses();
      pendingRequests.remove(msgID);
    } else {
      String errorMsg = "Node " + nodeID + " not present in the group. Ignoring Message : " + msg;
      logger.error(errorMsg);
      throw new GroupException(errorMsg);
    }
    return groupResponse.getResponse(nodeID);
  }

  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    this.zapNodeRequestProcessor = processor;
  }

  public void zapNode(NodeID nodeID, int type, String reason) {
    Member m = (Member) nodes.get(nodeID);
    if (m != null && zapNodeRequestProcessor.acceptOutgoingZapNodeRequest(nodeID, type, reason)) {
      long weights[] = zapNodeRequestProcessor.getCurrentNodeWeights();
      logger.warn("Zapping node : " + nodeID + " type = " + type + " reason = " + reason + " my weight = "
                  + Arrays.toString(weights));
      GroupMessage msg = GroupZapNodeMessageFactory.createGroupZapNodeMessage(type, reason, weights);
      try {
        sendTo(nodeID, msg);
      } catch (GroupException e) {
        logger.error("Error sending ZapNode Request to " + nodeID + " msg = " + msg);
      }
      logger.warn("Removing member " + m + " from group");
      memberDisappeared(m);
    } else {
      logger.warn("Ignoring Zap node request since either Member " + m + " is null or " + zapNodeRequestProcessor
                  + " asked us to : " + nodeID + " type = " + type + " reason = " + reason);
    }
  }

  private static class GroupResponseImpl implements GroupResponse {

    HashSet<NodeID>    waitFor   = new HashSet<NodeID>();
    List<GroupMessage> responses = new ArrayList<GroupMessage>();

    public synchronized List getResponses() {
      Assert.assertTrue(waitFor.isEmpty());
      return responses;
    }

    public synchronized GroupMessage getResponse(NodeID nodeID) {
      Assert.assertTrue(waitFor.isEmpty());
      for (Iterator<GroupMessage> i = responses.iterator(); i.hasNext();) {
        GroupMessage msg = i.next();
        if (nodeID.equals(msg.messageFrom())) return msg;
      }
      return null;
    }

    public void sendTo(GroupChannel group, GroupMessage msg, Member[] m) {
      try {
        if (m.length > 0) {
          setUpWaitFor(m);
          group.send(m, msg, SEND_OPTIONS_NO_ACK);
        }
      } catch (ChannelException e) {
        logger.error("Error sending msg : " + msg, e);
        reconsileWaitFor(e);
      }
    }

    private synchronized void setUpWaitFor(Member[] m) {
      for (int i = 0; i < m.length; i++) {
        waitFor.add(makeNodeIDFrom(m[i]));
      }
    }

    public synchronized void addResponseFrom(Member sender, GroupMessage gmsg) {
      if (!waitFor.remove(makeNodeIDFrom(sender))) {
        String message = "Recd response from a member not in list : " + sender + " : waiting For : " + waitFor
                         + " msg : " + gmsg;
        logger.error(message);
        throw new AssertionError(message);
      }
      responses.add(gmsg);
      notifyAll();
    }

    public synchronized void notifyMemberDead(Member member) {
      waitFor.remove(makeNodeIDFrom(member));
      notifyAll();
    }

    public synchronized void waitForAllResponses() throws GroupException {
      int count = 0;
      while (!waitFor.isEmpty()) {
        try {
          this.wait(5000);
          if (++count > 1) {
            logger.warn("Still waiting for response from " + waitFor + ". Count = " + count);
          }
        } catch (InterruptedException e) {
          throw new GroupException(e);
        }
      }
    }

    private synchronized void reconsileWaitFor(ChannelException e) {
      FaultyMember fm[] = e.getFaultyMembers();
      for (int i = 0; i < fm.length; i++) {
        logger.warn("Removing faulty Member " + fm[i] + " from list");
        waitFor.remove(makeNodeIDFrom(fm[i].getMember()));
      }
      logger.info("Current waiting members = " + waitFor);
    }
  }

  private final class ZapNodeRequestRouter implements GroupMessageListener {

    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      GroupZapNodeMessage zapMsg = (GroupZapNodeMessage) msg;
      zapNodeRequestProcessor.incomingZapNodeRequest(msg.messageFrom(), zapMsg.getZapNodeType(), zapMsg.getReason(),
                                                     zapMsg.getWeights());
    }

  }

}
