/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
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

  public void send(TCNetworkMessage message);

  public void receive(TCByteBuffer[] msgData);

  public boolean isConnected();

  public NetworkStackID open() throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException,
      IOException, CommStackMismatchException;

  public void close();

  public TCSocketAddress getRemoteAddress();

  public TCSocketAddress getLocalAddress();

}
