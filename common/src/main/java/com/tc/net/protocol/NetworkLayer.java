/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.net.protocol;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.core.ProductID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.object.session.SessionID;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Generic network layer interface.
 */
public interface NetworkLayer {

  /**
   * These are the types of layers that are there and we will be building a flagset which would have the information
   * about all the layers present depending on the config ADD ALL THE LAYERS TYPE HERE
   */

  public static final short  TYPE_TRANSPORT_LAYER              = 1;                                                                         // 0000000000000001
  public static final short  TYPE_OOO_LAYER                    = 2;                                                                         // 0000000000000010
  public static final short  TYPE_CHANNEL_LAYER                = 4;                                                                         // 0000000000000100
  public static final short  TYPE_TEST_MESSAGE                 = -1;                                                                        // This
                                                                                                                                             // is
                                                                                                                                             // for
                                                                                                                                             // test
                                                                                                                                             // messages
  /**
   * These are the name of the layers that are there in the communication stack IF ANY LAYER TYPE IS ADDED ABOVE THEN
   * ADD ITS CORRESPONDING NAME BELOW
   */
  public static final String NAME_TRANSPORT_LAYER              = "Transport Layer";
  public static final String NAME_OOO_LAYER                    = "Once and Only Once Protocol Layer";
  public static final String NAME_CHANNEL_LAYER                = "Channel Layer";

  /**
   * These are just errors corresponding to the exact mismatch of OOO layer in server and client stacks
   */
  public static final String ERROR_OOO_IN_SERVER_NOT_IN_CLIENT = "Once and Only Once Protocol Layer is present in server but not in client";
  public static final String ERROR_OOO_IN_CLIENT_NOT_IN_SERVER = "Once and Only Once Protocol Layer is present in client but not in server";

  /**
   * this function gets the stackLayerFlag
   */
  public short getStackLayerFlag();

  /**
   * This function gets the name of the particular stack layer
   */
  public String getStackLayerName();

  public void setSendLayer(NetworkLayer layer);

  public void setReceiveLayer(NetworkLayer layer);

  public NetworkLayer getReceiveLayer();

  public void send(TCNetworkMessage message) throws IOException;

  public void receive(TCNetworkMessage msgData);

  public boolean isConnected();

  public NetworkStackID open(InetSocketAddress serverAddress) throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException,
      IOException, CommStackMismatchException;

  public void reset();

  public void close();

  public InetSocketAddress getRemoteAddress();

  public InetSocketAddress getLocalAddress();

  public ProductID getProductID();
  
  public ConnectionID getConnectionID();

  public SessionID getSessionID();
  
  public TCByteBufferOutputStream createOutput();
}
