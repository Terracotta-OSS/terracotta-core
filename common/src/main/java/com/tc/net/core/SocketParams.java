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
package com.tc.net.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.properties.TCPropertiesConsts;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Nothing particularly fancy, just a common place that defines the socket options we set for our connections
 */
class SocketParams {
  private static final Logger logger       = LoggerFactory.getLogger(SocketParams.class);

  private static final String   RECV_BUFFER  = "recv.buffer";
  private static final String   SEND_BUFFER  = "send.buffer";
  private static final String   TCP_NO_DELAY = "tcpnodelay";
  private static final String   KEEP_ALIVE   = "keepalive";

  private final int             recvBuffer;
  private final int             sendBuffer;
  private final boolean         tcpNoDelay;
  private final boolean         keepAlive;

  SocketParams() {
    TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(TCPropertiesConsts.NETCORE_CATEGORY);

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
