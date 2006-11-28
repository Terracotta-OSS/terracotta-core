/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;

public interface DSOChannelManagerMBean {

  public void addEventListener(ChannelManagerEventListener listener);

  public MessageChannel[] getChannels();

}
