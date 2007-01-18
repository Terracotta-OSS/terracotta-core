/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.protocol.tcm.MessageChannel;

public interface DSOChannelManagerMBean {

  public MessageChannel[] getActiveChannels();

  public void addEventListener(DSOChannelManagerEventListener listener);

}
