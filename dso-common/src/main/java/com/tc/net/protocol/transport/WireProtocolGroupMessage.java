/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.TCNetworkMessage;

import java.util.Iterator;

public interface WireProtocolGroupMessage extends WireProtocolMessage {

  public Iterator<TCNetworkMessage> getMessageIterator();

  public int getTotalMessageCount();

}
