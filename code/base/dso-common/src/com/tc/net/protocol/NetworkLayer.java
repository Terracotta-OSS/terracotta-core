/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Generic network layer interface.
 */
public interface NetworkLayer {
  
  public void setSendLayer(NetworkLayer layer);
  
  public void setReceiveLayer(NetworkLayer layer);
  
  public void send(TCNetworkMessage message);
  
  public void receive(TCByteBuffer[] msgData);
  
  public boolean isConnected();

  public NetworkStackID open() throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException;
  
  public void close();  
}