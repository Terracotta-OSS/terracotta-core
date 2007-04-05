package com.tc.net.groups;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.MembershipListener;
import org.apache.catalina.tribes.ChannelException.FaultyMember;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.StaticMembershipInterceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.membership.StaticMember;
import org.apache.catalina.tribes.transport.DataSender;
import org.apache.catalina.tribes.transport.ReceiverBase;
import org.apache.catalina.tribes.transport.ReplicationTransmitter;
import org.apache.catalina.tribes.util.UUIDGenerator;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TribesGroupManager implements GroupManager, ChannelListener, MembershipListener {
  private static final int                                SEND_TIMEOUT_MILLIS = 6000;

  private static final TCLogger                           logger              = TCLogging
                                                                                  .getLogger(TribesGroupManager.class);

  private final GroupChannel                              group;
  private TcpFailureDetector                              failuredetector;
  private Member                                          thisMember;
  private NodeID                                          thisNodeID;

  private final CopyOnWriteArrayList<GroupEventsListener> groupListeners      = new CopyOnWriteArrayList<GroupEventsListener>();
  private final Map<NodeID, Member>                       nodes               = Collections
                                                                                  .synchronizedMap(new HashMap<NodeID, Member>());
  private final Map<String, GroupMessageListener>         messageListeners    = new ConcurrentHashMap<String, GroupMessageListener>();
  private final Map<MessageID, GroupResponse>             pendingRequests     = new Hashtable<MessageID, GroupResponse>();

  private boolean                                         debug               = false;

  public TribesGroupManager() {
    group = new GroupChannel();
  }

  public NodeID join(final Node thisNode, final Node[] allNodes) throws GroupException {
    try {
      // set up static nodes
      StaticMembershipInterceptor smi = setupStaticMembers(thisNode, allNodes);
      group.addInterceptor(smi);

      // set up receiver
      ReceiverBase receiver = (ReceiverBase) group.getChannelReceiver();
      receiver.setAddress(thisNode.getHost());
      receiver.setPort(thisNode.getPort());
      receiver.setAutoBind(0);

      // set up failure detector
      failuredetector = new TcpFailureDetector();
      group.addInterceptor(failuredetector);

      // add listeners
      group.addMembershipListener(this);
      group.addChannelListener(this);

      // config send timeout
      ReplicationTransmitter transmitter = (ReplicationTransmitter) group.getChannelSender();
      DataSender sender = transmitter.getTransport();
      sender.setTimeout(SEND_TIMEOUT_MILLIS);

      // start services
      failuredetector.start(Channel.DEFAULT);
      group.start(Channel.SND_RX_SEQ | Channel.SND_TX_SEQ | Channel.MBR_RX_SEQ);

      return this.thisNodeID;
    } catch (ChannelException e) {
      logger.error(e);
      throw new GroupException(e);
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
    this.thisNodeID = new NodeID(thisMember.getName(), thisMember.getUniqueId());
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
    if (msg instanceof GroupMessage) { return true; }
    logger.warn("Rejecting unknown message : " + msg + " from " + sender.getName());
    return false;
  }

  public void messageReceived(Serializable msg, Member sender) {
    GroupMessage gmsg = (GroupMessage) msg;
    if (debug) {
      logger.info(this.thisNodeID + " recd msg " + gmsg.getMessageID() + " From " + sender.getName());
    }
    MessageID requestID = gmsg.inResponseTo();
    NodeID from = new NodeID(sender.getName(), sender.getUniqueId());
    gmsg.setMessageOrginator(from);
    if (requestID.isNull() || !notifyPendingRequests(requestID, gmsg, sender)) {
      fireMessageReceivedEvent(from, gmsg);
    }
  }

  private void setup(final Node thisNode, final Node[] allNodes) throws AssertionError {
    StaticMembershipInterceptor smi = setupStaticMembers(thisNode, allNodes);

    ReceiverBase receiver = (ReceiverBase) group.getChannelReceiver();
    receiver.setAddress(thisNode.getHost());
    receiver.setPort(thisNode.getPort());
    receiver.setAutoBind(0);

    failuredetector = new TcpFailureDetector();
    group.addInterceptor(failuredetector);
    group.addInterceptor(smi);
    group.addMembershipListener(this);
    group.addChannelListener(this);
  }

  private StaticMember makeMember(final Node node) {
    try {
      StaticMember rv = new StaticMember(node.getHost(), node.getPort(), 0);
      rv.setUniqueId(UUIDGenerator.randomUUID(true));
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
    NodeID newNode = new NodeID(member.getName(), member.getUniqueId());
    Member old;
    if ((old = nodes.put(newNode, member)) == null) {
      fireNodeEvent(newNode, true);
    } else {
      logger.warn("Member Added Event called for : " + newNode + " while it is still present in the list of nodes : "
                  + old + " : " + nodes);
    }
  }

  private void fireNodeEvent(NodeID newNode, boolean joined) {
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
    NodeID node = new NodeID(member.getName(), member.getUniqueId());
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
    // TODO :: Validate the options flag
    try {
      Member m[] = group.getMembers();
      if (m.length > 0) {
        group.send(m, msg, Channel.SEND_OPTIONS_DEFAULT);
      }
    } catch (ChannelException e) {
      throw new GroupException(e);
    }
  }

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) throws GroupException {
    if (debug) {
      logger.info(this.thisNodeID + " : Sending to ALL and Waiting for Response : " + msg.getMessageID());
    }
    GroupResponseImpl groupResponse = new GroupResponseImpl();
    MessageID msgID = msg.getMessageID();
    GroupResponse old = pendingRequests.put(msgID, groupResponse);
    Assert.assertNull(old);
    groupResponse.sendAll(group, msg);
    groupResponse.waitForAllResponses();
    pendingRequests.remove(msgID);
    return groupResponse;
  }

  public void sendTo(NodeID node, GroupMessage msg) throws GroupException {
    if (debug) {
      logger.info(this.thisNodeID + " : Sending to : " + node + " msg " + msg.getMessageID());
    }
    try {
      Member to[] = new Member[1];
      to[0] = nodes.get(node);
      if (to[0] != null) {
        // TODO :: Validate the options flag
        group.send(to, msg, Channel.SEND_OPTIONS_DEFAULT);
      } else {
        // TODO:: These could be exceptions
        logger.warn("Node " + node + " not present in the group. Ignoring Message : " + msg);
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
    to[0] = nodes.get(nodeID);
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

  public void zapNode(NodeID nodeID) {
    logger.warn("TODO::Zapping node : " + nodeID);
    // TODO:: Implement this
  }

  private static class GroupResponseImpl implements GroupResponse {

    HashSet<Member>    waitFor   = new HashSet<Member>();
    List<GroupMessage> responses = new ArrayList<GroupMessage>();

    public synchronized List getResponses() {
      Assert.assertTrue(waitFor.isEmpty());
      return responses;
    }

    public GroupMessage getResponse(NodeID nodeID) {
      Assert.assertTrue(waitFor.isEmpty());
      for (Iterator<GroupMessage> i = responses.iterator(); i.hasNext();) {
        GroupMessage msg = i.next();
        if (nodeID.equals(msg.messageFrom())) return msg;
      }
      return null;
    }

    public synchronized void sendTo(GroupChannel group, GroupMessage msg, Member[] m) {
      waitFor.addAll(Arrays.asList(m));
      try {
        if (m.length > 0) {
          group.send(m, msg, Channel.SEND_OPTIONS_DEFAULT);
        }
      } catch (ChannelException e) {
        logger.error("Error sending msg : " + msg, e);
        reconsileWaitFor(e);
      }
    }

    public synchronized void addResponseFrom(Member sender, GroupMessage gmsg) {
      Assert.assertNotNull(waitFor.remove(sender));
      responses.add(gmsg);
      notifyAll();
    }

    public synchronized void notifyMemberDead(Member member) {
      waitFor.remove(member);
      notifyAll();
    }

    public synchronized void waitForAllResponses() throws GroupException {
      while (!waitFor.isEmpty()) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          throw new GroupException(e);
        }
      }
    }

    public synchronized void sendAll(GroupChannel group, GroupMessage msg) {
      Member m[] = group.getMembers();
      sendTo(group, msg, m);
    }

    private void reconsileWaitFor(ChannelException e) {
      FaultyMember fm[] = e.getFaultyMembers();
      for (int i = 0; i < fm.length; i++) {
        waitFor.remove(fm[i].getMember());
      }
    }
  }

}
