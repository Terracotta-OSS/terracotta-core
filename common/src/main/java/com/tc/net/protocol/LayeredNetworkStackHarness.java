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
package com.tc.net.protocol;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.MessageTransport;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public abstract class LayeredNetworkStackHarness implements NetworkStackHarness {
  
  private final List<NetworkLayer>          intermediate = new LinkedList<NetworkLayer>();
  /**
   * This used to connect the intermediate layers to transport on one end and 
   * the channel on the other.  The layers are always connected channel -> transport 
   * in the send direction and transport -> channel in the receive direction.  The network
   * layer returned at the end needs to be connected to the end channel of the stack 
   * is the same manner as described here.
   * 
  */
  protected NetworkLayer chainNetworkLayers(MessageTransport transport) {
    NetworkLayer last = transport;
    for (NetworkLayer next : intermediate) {
      last.setReceiveLayer(next);
      next.setSendLayer(last);
      last = next;
    }
    return last;
  }
  /**
   * adds a network layer in order where the added layer is connected 
   * next in line from the transport going to the channel
  */
  public void appendIntermediateLayer(NetworkLayer layer) {
    intermediate.add(layer);
  }  
  
  public abstract MessageTransport getTransport();
  
  public abstract MessageChannel getChannel();
}
