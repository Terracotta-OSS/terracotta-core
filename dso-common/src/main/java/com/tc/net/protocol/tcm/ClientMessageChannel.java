/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

public interface ClientMessageChannel extends MessageChannel {

  public int getConnectCount();

  public int getConnectAttemptCount();

  public ChannelIDProvider getChannelIDProvider();
}
