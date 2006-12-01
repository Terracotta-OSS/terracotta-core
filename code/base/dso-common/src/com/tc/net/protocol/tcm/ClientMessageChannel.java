/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;

public interface ClientMessageChannel extends MessageChannel {

  public void addClassMapping(TCMessageType type, Class msgClass);

  public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink);

  public void routeMessageType(TCMessageType type, TCMessageSink sink);
  
  public void unrouteMessageType(TCMessageType type);

  public int getConnectCount();

  public int getConnectAttemptCount();

  public ChannelIDProvider getChannelIDProvider();
  
}
