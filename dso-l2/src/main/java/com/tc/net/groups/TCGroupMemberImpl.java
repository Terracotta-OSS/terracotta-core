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
package com.tc.net.groups;

import com.tc.l2.L2DebugLogging;
import com.tc.l2.L2DebugLogging.LogLevel;
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

  @Override
  public MessageChannel getChannel() {
    return channel;
  }

  /*
   * Use a wrapper to send old tribes GroupMessage out through channel's TCMessage
   */
  @Override
  public void send(GroupMessage msg) throws GroupException {
    if (!channel.isOpen()) { throw new GroupException("Channel is not ready: " + toString()); }
    sendMessage(msg);
  }

  @Override
  public void sendIgnoreNotReady(GroupMessage msg) {
    if (!channel.isOpen()) {
      logger.warn("Attempting send to a not ready member " + this + ", msg will not be sent: " + msg);
      return;
    }
    sendMessage(msg);
  }

  private void sendMessage(GroupMessage msg) {
    TCGroupMessageWrapper wrapper = (TCGroupMessageWrapper) channel.createMessage(TCMessageType.GROUP_WRAPPER_MESSAGE);
    wrapper.setGroupMessage(msg);
    wrapper.send();
  }

  @Override
  public String toString() {
    return ("Group Member: " + localNodeID + " <-> " + peerNodeID + " " + channel + "; Ready:" + ready + "; Joined: "
            + joined + "; memberAdding:" + memberAdding + "; HighPri: " + isHighPriorityNode());
  }

  @Override
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

  @Override
  public ServerID getLocalNodeID() {
    return localNodeID;
  }

  @Override
  public ServerID getPeerNodeID() {
    return peerNodeID;
  }

  @Override
  public void setTCGroupManager(TCGroupManagerImpl manager) {
    this.manager = manager;
  }

  @Override
  public TCGroupManagerImpl getTCGroupManager() {
    return manager;
  }

  @Override
  public boolean isReady() {
    waitForMemberAdded();
    return (ready.get());
  }

  @Override
  public void setReady(boolean isReady) {
    ready.set(isReady);
  }

  @Override
  public boolean isJoinedEventFired() {
    return (joined.get());
  }

  @Override
  public void setJoinedEventFired(boolean isReady) {
    joined.set(isReady);
  }

  @Override
  public void close() {
    ready.set(false);
    debugInfo("Closing channel: " + channel);
    getChannel().close();
  }

  @Override
  public boolean isHighPriorityNode() {
    return (localNodeID.compareTo(peerNodeID) > 0);
  }

  @Override
  public synchronized void memberAddingInProcess() {
    memberAdding = true;
  }

  @Override
  public synchronized void abortMemberAdding() {
    if (memberAdding) {
      memberAdding = false;
      notifyAll();
    }
  }

  @Override
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

  private static void debugInfo(String message) {
    L2DebugLogging.log(logger, LogLevel.INFO, message, null);
  }
}