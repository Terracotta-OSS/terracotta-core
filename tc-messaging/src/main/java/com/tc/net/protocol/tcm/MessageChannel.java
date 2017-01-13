/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.tcm;

import com.tc.util.ProductID;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;

/**
 * Outward facing message channel interface. This is the interface that most high level application code wants to deal
 * with, as opposed to the MessageChannelInternal interface which has some extra noise
 * 
 * @author teck
 */
public interface MessageChannel extends ChannelIDProvider {

  public TCSocketAddress getLocalAddress();

  public TCSocketAddress getRemoteAddress();

  public void addListener(ChannelEventListener listener);
  
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

  public void send(TCNetworkMessage message) throws IOException;
  
  public NetworkStackID open(ConnectionInfo info) throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException, CommStackMismatchException;

  public NetworkStackID open(Collection<ConnectionInfo> info) throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException, CommStackMismatchException;

  public NetworkStackID open(Collection<ConnectionInfo> info, String username, char[] password) throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException, CommStackMismatchException;

  public void close();
  
  public NodeID getLocalNodeID();
  
  public void setLocalNodeID(NodeID source);
  
  public NodeID getRemoteNodeID();

  public ProductID getProductId();
}
