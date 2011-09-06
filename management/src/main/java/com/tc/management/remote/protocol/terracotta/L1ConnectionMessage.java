/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.async.api.EventContext;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.util.UUID;

import java.util.concurrent.ConcurrentMap;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnector;

public class L1ConnectionMessage implements EventContext {

  public static final class Connecting extends L1ConnectionMessage {
    public Connecting(MBeanServer mbs, MessageChannel channel, UUID uuid, String[] tunneledDomains,
                      ConcurrentMap<ChannelID, JMXConnector> channelIdToJmxConnector,
                      ConcurrentMap<ChannelID, TunnelingMessageConnection> channelIdToMsgConnection) {
      super(mbs, channel, uuid, tunneledDomains, channelIdToJmxConnector, channelIdToMsgConnection);
    }
  }

  public static final class Disconnecting extends L1ConnectionMessage {
    public Disconnecting(MessageChannel channel, ConcurrentMap<ChannelID, JMXConnector> channelIdToJmxConnector,
                         ConcurrentMap<ChannelID, TunnelingMessageConnection> channelIdToMsgConnection) {
      super(channel, channelIdToJmxConnector, channelIdToMsgConnection);
    }
  }

  private final MBeanServer                                          mbs;
  private final MessageChannel                                       channel;
  private final UUID                                                 uuid;
  private final String[]                                             tunneledDomains;
  private final ConcurrentMap<ChannelID, JMXConnector>               channelIdToJmxConnector;
  private final ConcurrentMap<ChannelID, TunnelingMessageConnection> channelIdToMsgConnection;
  private final boolean                                              isConnectingMsg;

  private L1ConnectionMessage(MBeanServer mbs, MessageChannel channel, UUID uuid, String[] tunneledDomains,
                              ConcurrentMap<ChannelID, JMXConnector> channelIdToJmxConnector,
                              ConcurrentMap<ChannelID, TunnelingMessageConnection> channelIdToMsgConnection) {
    this.mbs = mbs;
    this.channel = channel;
    this.uuid = uuid;
    this.tunneledDomains = tunneledDomains;
    this.channelIdToJmxConnector = channelIdToJmxConnector;
    this.channelIdToMsgConnection = channelIdToMsgConnection;
    this.isConnectingMsg = true;

    if (isConnectingMsg && mbs == null) {
      final AssertionError ae = new AssertionError("Attempting to create a L1-connecting-message without"
                                                   + " a valid mBeanServer.");
      throw ae;
    }
  }

  private L1ConnectionMessage(MessageChannel channel, ConcurrentMap<ChannelID, JMXConnector> channelIdToJmxConnector,
                              ConcurrentMap<ChannelID, TunnelingMessageConnection> channelIdToMsgConnection) {
    this.mbs = null;
    this.channel = channel;
    this.uuid = null;
    this.tunneledDomains = null;
    this.channelIdToJmxConnector = channelIdToJmxConnector;
    this.channelIdToMsgConnection = channelIdToMsgConnection;
    this.isConnectingMsg = false;

    if (isConnectingMsg && mbs == null) {
      final AssertionError ae = new AssertionError("Attempting to create a L1-disconnecting-message without"
                                                   + " a valid mBeanServer.");
      throw ae;
    }
  }

  public MBeanServer getMBeanServer() {
    return mbs;
  }

  public MessageChannel getChannel() {
    return channel;
  }

  public UUID getUUID() {
    return uuid;
  }

  public String[] getTunneledDomains() {
    return tunneledDomains;
  }

  public ConcurrentMap<ChannelID, JMXConnector> getChannelIdToJmxConnector() {
    return channelIdToJmxConnector;
  }

  public ConcurrentMap<ChannelID, TunnelingMessageConnection> getChannelIdToMsgConnector() {
    return channelIdToMsgConnection;
  }

  public boolean isConnectingMsg() {
    return isConnectingMsg;
  }
}
