/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

public interface ClientMessageChannel extends MessageChannel {

  public int getConnectCount();

  public int getConnectAttemptCount();

  public ChannelIDProvider getChannelIDProvider();
}
