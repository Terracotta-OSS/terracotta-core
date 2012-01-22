/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

public class ChannelEventType {
  public static final ChannelEventType TRANSPORT_DISCONNECTED_EVENT          = new ChannelEventType(
                                                                                                    "TRANSPORT_DISCONNCTED_EVENT");
  public static final ChannelEventType TRANSPORT_CONNECTED_EVENT             = new ChannelEventType(
                                                                                                    "TRANSPORT_CONNECTED_EVENT");
  public static final ChannelEventType CHANNEL_CLOSED_EVENT                  = new ChannelEventType(
                                                                                                    "CHANNEL_CLOSED_EVENT");
  public static final ChannelEventType CHANNEL_OPENED_EVENT                  = new ChannelEventType(
                                                                                                    "CHANNEL_OPENED_EVENT");
  public static final ChannelEventType TRANSPORT_RECONNECTION_REJECTED_EVENT = new ChannelEventType(
                                                                                                    "TRANSPORT_RECONNECTION_REJECT_EVENT");

  private final String                 name;

  private ChannelEventType(String name) {
    this.name = name;
  }

  public boolean matches(ChannelEvent event) {
    return event == null ? false : event.getType() == this;
  }

  @Override
  public String toString() {
    return name;
  }
}
