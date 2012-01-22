/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;

public interface DSOChannelManagerMBean {

  public MessageChannel[] getActiveChannels();

  public void addEventListener(DSOChannelManagerEventListener listener);
  
  public ClientID getClientIDFor(ChannelID channelID);

}
