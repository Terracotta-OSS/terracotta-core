/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ServerID;
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
  private static final TCLogger logger       = TCLogging.getLogger(TCGroupMemberImpl.class);
  private TCGroupManagerImpl    manager;
  private final MessageChannel  channel;
  private final ServerID        localNodeID;
  private final ServerID        peerNodeID;
  // set member ready only when both ends are in group
  private final AtomicBoolean   ready        = new AtomicBoolean(false);
  private final AtomicBoolean   joined       = new AtomicBoolean(false);
  private volatile boolean      memberAdding = false;

  public TCGroupMemberImpl(ServerID localNodeID, ServerID peerNodeID, MessageChannel channel) {
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
    sendMessage(msg);
  }

  public void sendIgnoreNotReady(GroupMessage msg) {
    if (!channel.isOpen()) {
      logger.warn("Send to a not ready member " + this);
      return;
    }
    sendMessage(msg);
  }

  private void sendMessage(GroupMessage msg) {
    TCGroupMessageWrapper wrapper = (TCGroupMessageWrapper) channel.createMessage(TCMessageType.GROUP_WRAPPER_MESSAGE);
    wrapper.setGroupMessage(msg);
    wrapper.send();
  }

  public String toString() {
    return ("Group Member: " + localNodeID + " <-> " + peerNodeID + " " + channel + "; Ready:" + ready + "; Joined: "
            + joined + "; memberAdding:" + memberAdding + "; HighPri: " + isHighPriorityNode());
  }

  public void notifyChannelEvent(ChannelEvent event) {
    if (event.getChannel() == channel) {
      if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
        if (isJoinedEventFired()) {
          ready.set(true);
        } else {
          // Ignore tx connect event before the member join.
        }
      } else if ((event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT)
                 || (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT)) {
        ready.set(false);
      }
    }
  }

  public ServerID getLocalNodeID() {
    return localNodeID;
  }

  public ServerID getPeerNodeID() {
    return peerNodeID;
  }

  public void setTCGroupManager(TCGroupManagerImpl manager) {
    this.manager = manager;
  }

  public TCGroupManagerImpl getTCGroupManager() {
    return manager;
  }

  public boolean isReady() {
    waitForMemberAdded();
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
    getChannel().close();
  }

  public boolean isHighPriorityNode() {
    return (localNodeID.compareTo(peerNodeID) > 0);
  }

  public synchronized void memberAddingInProcess() {
    memberAdding = true;
  }

  public synchronized void abortMemberAdding() {
    if (memberAdding) {
      memberAdding = false;
      notifyAll();
    }
  }

  public synchronized void notifyMemberAdded() {
    memberAdding = false;
    notifyAll();
  }

  private synchronized void waitForMemberAdded() {
    while (memberAdding) {
      try {
        wait();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

}