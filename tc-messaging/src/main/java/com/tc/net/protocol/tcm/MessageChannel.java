/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.tcm;

import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.core.ProductID;
import com.tc.object.session.SessionID;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Outward facing message channel interface. This is the interface that most high level application code wants to deal
 * with, as opposed to the MessageChannelInternal interface which has some extra noise
 * 
 * @author teck
 */
public interface MessageChannel {

  public InetSocketAddress getLocalAddress();

  public InetSocketAddress getRemoteAddress();

  public void addListener(ChannelEventListener listener);
  
  public boolean isOpen();

  public boolean isClosed();

  public TCAction createMessage(TCMessageType type);
  
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

  public void send(TCNetworkMessage message) throws IOException;
  
  public NetworkStackID open(InetSocketAddress serverAddress) throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException, CommStackMismatchException;

  public NetworkStackID open(Iterable<InetSocketAddress> serverAddresses) throws MaxConnectionsExceededException,
      TCTimeoutException,
      UnknownHostException, IOException, CommStackMismatchException;

  public void close();
  
  public NodeID getLocalNodeID();
  
  public void setLocalNodeID(NodeID source);
  
  public NodeID getRemoteNodeID();
  
  public ProductID getProductID();
  
  public ConnectionID getConnectionID();
  
  public ChannelID getChannelID();

  public SessionID getSessionID();
}
