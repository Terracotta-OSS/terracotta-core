/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;

import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Each TCGroupMember sits on top of a channel.
 */
public class TCGroupMemberImpl implements TCGroupMember, ChannelEventListener {
  private TCGroupManagerImpl   manager;
  private final MessageChannel channel;
  private final NodeIDImpl     localNodeID;
  private final NodeIDImpl     peerNodeID;
  // set member ready only when both ends are in group
  private final AtomicBoolean  ready              = new AtomicBoolean(false);
  private final AtomicBoolean  joined             = new AtomicBoolean(false);
  private volatile boolean     closeEventNotified = false;
  private volatile boolean     eventFiring        = false;

  public TCGroupMemberImpl(NodeIDImpl localNodeID, NodeIDImpl peerNodeID, MessageChannel channel) {
    this.channel = channel;
    this.localNodeID = localNodeID;
    this.peerNodeID = peerNodeID;
    this.channel.addListener(this);
  }

  public MessageChannel getChannel() {
    return channel;
  }

  /*
   * Use a wrapper to send old tribes GroupMessage out through channel's TCMessage
   */
  public void send(GroupMessage msg) throws GroupException {
    if (!channel.isOpen()) { throw new GroupException("Channel is not ready: " + toString()); }
    TCGroupMessageWrapper wrapper = (TCGroupMessageWrapper) channel.createMessage(TCMessageType.GROUP_WRAPPER_MESSAGE);
    wrapper.setGroupMessage(msg);
    wrapper.send();
  }

  public String toString() {
    return ("Group Member: " + localNodeID + " <-> " + peerNodeID);
  }

  public void notifyChannelEvent(ChannelEvent event) {
    if (event.getChannel() == channel) {
      if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
        ready.set(true);
      } else if ((event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT)
                 || (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT)) {
        ready.set(false);
        closeEventNotified = true;
      }
    }
  }

  public NodeIDImpl getLocalNodeID() {
    return localNodeID;
  }

  public NodeIDImpl getPeerNodeID() {
    return peerNodeID;
  }

  public void setTCGroupManager(TCGroupManagerImpl manager) {
    this.manager = manager;
  }

  public TCGroupManagerImpl getTCGroupManager() {
    return manager;
  }

  public boolean isReady() {
    waitForEventFired();
    return (ready.get());
  }

  public void setReady(boolean isReady) {
    ready.set(isReady);
  }

  public boolean isJoinedEventFired() {
    return (joined.get());
  }

  public void setJoinedEventFired(boolean isReady) {
    joined.set(isReady);
  }

  public void close() {
    ready.set(false);
    if (!closeEventNotified) getChannel().close();
  }

  public boolean isHighPriorityNode() {
    return (localNodeID.compareTo(peerNodeID) > 0);
  }

  public synchronized void eventFiringInProcess() {
    eventFiring = true;
  }

  public synchronized void abortEventFiring() {
    if (eventFiring) {
      eventFiring = false;
      notifyAll();
    }
  }

  public synchronized void notifyEventFired() {
    eventFiring = false;
    notifyAll();
  }

  private synchronized void waitForEventFired() {
    while (eventFiring) {
      try {
        wait();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

}