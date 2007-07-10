/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

public class ChannelEventType {
  public static final ChannelEventType TRANSPORT_DISCONNECTED_EVENT = new ChannelEventType("TRANSPORT_DISCONNCTED_EVENT");
  public static final ChannelEventType TRANSPORT_DISRUPTED_EVENT    = new ChannelEventType("TRANSPORT_DISRUPTED_EVENT");
  public static final ChannelEventType TRANSPORT_RESTORED_EVENT    = new ChannelEventType("TRANSPORT_RESTORED_EVENT");
  public static final ChannelEventType TRANSPORT_CONNECTED_EVENT    = new ChannelEventType("TRANSPORT_CONNECTED_EVENT");
  public static final ChannelEventType CHANNEL_CLOSED_EVENT         = new ChannelEventType("CHANNEL_CLOSED_EVENT");
  public static final ChannelEventType CHANNEL_OPENED_EVENT         = new ChannelEventType("CHANNEL_OPENED_EVENT");

  private final String                 name;

  private ChannelEventType(String name) {
    this.name = name;
  }

  public boolean matches(ChannelEvent event) {
    return event == null ? false : event.getType() == this;
  }

  public String toString() {
    return name;
  }
}