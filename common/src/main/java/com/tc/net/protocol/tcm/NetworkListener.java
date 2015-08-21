/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.protocol.transport.ConnectionID;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;

public interface NetworkListener {

  public void start(Set<ConnectionID> initialConnectionIDs) throws IOException;

  public void stop(long timeout) throws TCTimeoutException;

  public ChannelManager getChannelManager();

  public InetAddress getBindAddress();

  public int getBindPort();

}
