/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.util.UUID;

import javax.management.MBeanServer;

public class L1ConnectionMessage {

  public static final class Connecting extends L1ConnectionMessage {
    public Connecting(MBeanServer mbs, MessageChannel channel, UUID uuid, String[] tunneledDomains) {
      super(mbs, channel, uuid, tunneledDomains);
    }
  }

  public static final class Disconnecting extends L1ConnectionMessage {
    public Disconnecting(MessageChannel channel) {
      super(channel);
    }
  }

  private final MBeanServer                                          mbs;
  private final MessageChannel                                       channel;
  private final UUID                                                 uuid;
  private final String[]                                             tunneledDomains;
  private final boolean                                              isConnectingMsg;

  private L1ConnectionMessage(MBeanServer mbs, MessageChannel channel, UUID uuid, String[] tunneledDomains) {
    this.mbs = mbs;
    this.channel = channel;
    this.uuid = uuid;
    this.tunneledDomains = tunneledDomains;
    this.isConnectingMsg = true;

    if (isConnectingMsg && mbs == null) {
      final AssertionError ae = new AssertionError("Attempting to create a L1-connecting-message without"
                                                   + " a valid mBeanServer.");
      throw ae;
    }
  }

  private L1ConnectionMessage(MessageChannel channel) {
    this.mbs = null;
    this.channel = channel;
    this.uuid = null;
    this.tunneledDomains = null;
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

  public boolean isConnectingMsg() {
    return isConnectingMsg;
  }
}
