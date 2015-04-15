/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Nothing particularly fancy, just a common place that defines the socket options we set for our connections
 */
class SocketParams {
  private static final TCLogger logger       = TCLogging.getLogger(SocketParams.class);

  private static final String   PREFIX       = "net.core";
  private static final String   RECV_BUFFER  = "recv.buffer";
  private static final String   SEND_BUFFER  = "send.buffer";
  private static final String   TCP_NO_DELAY = "tcpnodelay";
  private static final String   KEEP_ALIVE   = "keepalive";

  private final int             recvBuffer;
  private final int             sendBuffer;
  private final boolean         tcpNoDelay;
  private final boolean         keepAlive;

  SocketParams() {
    TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(PREFIX);

    this.recvBuffer = props.getInt(RECV_BUFFER, -1);
    this.sendBuffer = props.getInt(SEND_BUFFER, -1);
    this.keepAlive = props.getBoolean(KEEP_ALIVE);
    this.tcpNoDelay = props.getBoolean(TCP_NO_DELAY);
  }

  void applySocketParams(Socket s) {
    if (sendBuffer > 0) {
      try {
        s.setSendBufferSize(sendBuffer);
      } catch (SocketException e) {
        logger.error("error setting sendBuffer to " + sendBuffer, e);
      }
    }

    if (recvBuffer > 0) {
      try {
        s.setReceiveBufferSize(recvBuffer);
      } catch (SocketException e) {
        logger.error("error setting recvBuffer to " + recvBuffer, e);
      }
    }

    try {
      s.setTcpNoDelay(tcpNoDelay);
    } catch (SocketException e) {
      logger.error("error setting TcpNoDelay to " + tcpNoDelay, e);
    }

    try {
      s.setKeepAlive(keepAlive);
    } catch (SocketException e) {
      logger.error("error setting KeepAlive to " + keepAlive, e);
    }
  }

  void applyServerSocketParams(ServerSocket s, boolean reuseAddress) {

    try {
      s.setReuseAddress(reuseAddress);
    } catch (SocketException e) {
      logger.error("error setting recvBuffer to " + recvBuffer, e);
    }

    if (recvBuffer > 0) {
      try {
        s.setReceiveBufferSize(recvBuffer);
      } catch (SocketException e) {
        logger.error("error setting recvBuffer to " + recvBuffer, e);
      }
    }
  }

}
