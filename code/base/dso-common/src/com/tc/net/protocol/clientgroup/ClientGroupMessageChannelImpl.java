/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.clientgroup;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventImpl;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.ClientMessageChannelImpl;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageFactory;
import com.tc.net.protocol.tcm.TCMessageImpl;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.object.session.SessionProvider;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientGroupMessageChannelImpl extends ClientMessageChannelImpl implements ClientGroupMessageChannel {
  private static final TCLogger       logger = TCLogging.getLogger(ClientGroupMessageChannel.class);
  private final TCMessageFactory      msgFactory;
  private final SessionProvider       sessionProvider;

  private final CommunicationsManager communicationsManager;
  private final Map                   groupChannelMap;
  private final GroupID               coordinatorGroupID;

  public ClientGroupMessageChannelImpl(TCMessageFactory msgFactory, SessionProvider sessionProvider,
                                       final int maxReconnectTries, CommunicationsManager communicationsManager,
                                       ConnectionAddressProvider[] addressProviders) {
    super(msgFactory, null, sessionProvider, GroupID.ALL_GROUPS);
    this.msgFactory = msgFactory;
    this.sessionProvider = sessionProvider;
    this.communicationsManager = communicationsManager;

    logger.info("Create active channels");
    Assert.assertTrue(addressProviders.length > 0);
    Map channels = new LinkedHashMap();
    coordinatorGroupID = createSubChannel(channels, maxReconnectTries, addressProviders[0]);
    for (int i = 1; i < addressProviders.length; ++i) {
      createSubChannel(channels, maxReconnectTries, addressProviders[i]);
    }
    this.groupChannelMap = Collections.unmodifiableMap(channels);
  }

  private GroupID createSubChannel(Map channels, final int maxReconnectTries, ConnectionAddressProvider addressProvider) {
    ClientMessageChannel channel = this.communicationsManager
        .createClientChannel(this.sessionProvider, maxReconnectTries, null, 0, 10000, addressProvider,
                             null, this.msgFactory,
                             new TCMessageRouterImpl());
    GroupID groupID = (GroupID) channel.getRemoteNodeID();
    channels.put(groupID, channel);
    logger.info("Created sub-channel " + groupID + ": " + addressProvider);
    return groupID;
  }

  public ClientMessageChannel getActiveCoordinator() {
    return getChannel(coordinatorGroupID);
  }

  public ChannelID getActiveActiveChannelID() {
    return getActiveCoordinator().getChannelID();
  }

  public ClientMessageChannel getChannel(GroupID groupID) {
    return (ClientMessageChannel) groupChannelMap.get(groupID);
  }

  public GroupID[] getGroupIDs() {
    return (GroupID[]) groupChannelMap.keySet().toArray(new GroupID[groupChannelMap.size()]);
  }

  public TCMessage createMessage(NodeID nodeID, TCMessageType type) {
    ClientMessageChannel ch = (ClientMessageChannel) groupChannelMap.get(nodeID);
    if (ch == null) { throw new AssertionError("ClientMessageChannel is null for " + nodeID + " : Type : " + type); }
    TCMessage rv = msgFactory.createMessage(ch, type);
    return rv;
  }

  public TCMessage createMessage(TCMessageType type) {
    return createMessage(coordinatorGroupID, type);
  }

  private String connectionInfo(ClientMessageChannel ch) {
    return (ch.getLocalAddress() + " -> " + ch.getRemoteAddress());
  }

  public NetworkStackID open() throws TCTimeoutException, UnknownHostException, IOException,
      MaxConnectionsExceededException {
    NetworkStackID nid = null;
    ClientMessageChannel ch = null;
    try {
      // open coordinator channel
      ch = getChannel(coordinatorGroupID);
      if (!ch.isOpen()) {
        nid = ch.open();
        setLocalNodeID(new ClientID(getChannelID()));
        logger.info("Opened sub-channel(coordinator): " + connectionInfo(ch));
      }

      for (Iterator i = groupChannelMap.keySet().iterator(); i.hasNext();) {
        GroupID id = (GroupID) i.next();
        if (id == coordinatorGroupID) continue;
        ch = getChannel(id);
        if (ch.isOpen()) continue;
        ch.setLocalNodeID(getLocalNodeID());
        ch.open();
        logger.info("Opened sub-channel: " + connectionInfo(ch));
      }
    } catch (TCTimeoutException e) {
      throw new TCTimeoutException(connectionInfo(ch) + " " + e);
    } catch (UnknownHostException e) {
      throw new UnknownHostException(connectionInfo(ch) + " " + e);
    } catch (MaxConnectionsExceededException e) {
      throw new MaxConnectionsExceededException(connectionInfo(ch) + " " + e);
    }

    logger.info("All active sub-channels opened");
    return nid;
  }

  public ChannelID getChannelID() {
    // return one of active-coordinator, they are same for all channels
    return getActiveCoordinator().getChannelID();
  }

  public int getConnectCount() {
    // an aggregate of all channels
    int count = 0;
    for (Iterator i = groupChannelMap.keySet().iterator(); i.hasNext();) {
      count += getChannel((GroupID) i.next()).getConnectCount();
    }
    return count;
  }

  public int getConnectAttemptCount() {
    // an aggregate of all channels
    int count = 0;
    for (Iterator i = groupChannelMap.keySet().iterator(); i.hasNext();) {
      count += getChannel((GroupID) i.next()).getConnectAttemptCount();
    }
    return count;
  }

  public void routeMessageType(TCMessageType messageType, TCMessageSink dest) {
    for (Iterator i = groupChannelMap.keySet().iterator(); i.hasNext();) {
      getChannel((GroupID) i.next()).routeMessageType(messageType, dest);
    }
  }

  /*
   * broadcast messages
   */
  public void broadcast(final TCMessageImpl message) {
    message.dehydrate();
    for (Iterator i = groupChannelMap.keySet().iterator(); i.hasNext();) {
      TCMessageImpl tcMesg = (TCMessageImpl) getChannel((GroupID) i.next()).createMessage(message.getMessageType());
      tcMesg.cloneAndSend(message);
    }
    message.wasSent();
  }

  public void send(final TCNetworkMessage message) {
    getActiveCoordinator().send(message);
  }

  public TCSocketAddress getRemoteAddress() {
    return getActiveCoordinator().getRemoteAddress();
  }

  public void notifyTransportConnected(MessageTransport transport) {
    throw new AssertionError();
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    throw new AssertionError();
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    throw new AssertionError();
  }

  public void notifyTransportClosed(MessageTransport transport) {
    throw new AssertionError();
  }

  public ChannelIDProvider getChannelIDProvider() {
    // return one from active-coordinator
    return getActiveCoordinator().getChannelIDProvider();
  }

  public void close() {
    for (Iterator i = groupChannelMap.keySet().iterator(); i.hasNext();) {
      getChannel((GroupID) i.next()).close();
    }
  }

  public boolean isConnected() {
    if (groupChannelMap.size() == 0) return false;
    for (Iterator i = groupChannelMap.keySet().iterator(); i.hasNext();) {
      if (!getChannel((GroupID) i.next()).isConnected()) return false;
    }
    return true;
  }

  public boolean isOpen() {
    if (groupChannelMap.size() == 0) return false;
    for (Iterator i = groupChannelMap.keySet().iterator(); i.hasNext();) {
      if (!getChannel((GroupID) i.next()).isOpen()) return false;
    }
    return true;
  }

  /*
   * As a middleman between ClientHandshakeManager and multiple ClientMessageChannels. Bookkeeping sub-channels' events
   * Notify connected every channel connected. Notify disconnected on every channel disconnected
   */
  private class ClientGroupMessageChannelEventListener implements ChannelEventListener {
    private final ChannelEventListener listener;

    public ClientGroupMessageChannelEventListener(ChannelEventListener listener) {
      this.listener = listener;
    }

    public void notifyChannelEvent(ChannelEvent event) {
      fireEvent(event);
    }

    private void fireEvent(ChannelEvent event) {
      listener.notifyChannelEvent(new ChannelEventImpl(event.getType(), event.getChannel()));
    }
  }

  public void addListener(ChannelEventListener listener) {
    ClientGroupMessageChannelEventListener middleman = new ClientGroupMessageChannelEventListener(listener);
    for (Iterator i = groupChannelMap.keySet().iterator(); i.hasNext();) {
      getChannel((GroupID) i.next()).addListener(middleman);
    }
  }

  public GroupID getCoordinatorGroupID() {
    return coordinatorGroupID;
  }

}