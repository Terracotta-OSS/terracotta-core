/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Outward facing message channel interface. This is the interface that most high level application code wants to deal
 * with, as opposed to the MessageChannelInternal interface which has some extra noise
 * 
 * @author teck
 */
public interface MessageChannel {

  public TCSocketAddress getLocalAddress();

  public TCSocketAddress getRemoteAddress();

  public void addListener(ChannelEventListener listener);

  public ChannelID getChannelID();
  
  public boolean isOpen();

  public boolean isClosed();

  public TCMessage createMessage(TCMessageType type);
  
  public Object getAttachment(String key);

  /**
   * Attach anonymous data to this channel with the given key
   * 
   * @param key the key for the attachment
   * @param value the data to attach
   * @param replace true if we should not check if a mapping already exists, else false
   */
  public void addAttachment(String key, Object value, boolean replace);

  /**
   * Remove anonymous data from this channel
   * 
   * @return the attachement object removed (if any)
   */
  public Object removeAttachment(String key);

  // ////////////////////////////////
  // Methods duplicated from NetworkLayer
  // ////////////////////////////////
  public boolean isConnected();

  public void send(TCNetworkMessage message);

  public NetworkStackID open() throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException,
      IOException, CommStackMismatchException;

  public void close();
  
  public NodeID getLocalNodeID();
  
  public void setLocalNodeID(NodeID source);
  
  public NodeID getRemoteNodeID();
  
}