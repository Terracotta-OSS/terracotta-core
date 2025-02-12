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

import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.NetworkMessageSink;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Common interface for TC network connections
 * 
 * @author teck
 */
public interface TCConnection extends NetworkMessageSink {

  /**
   * Returns a long timestamp as reported by <code>Sytem.currentTimeMillis()</code> when this connection was connected.
   * If <code>connect()</code> has never been called, the return value is undefined
   */
  public long getConnectTime();

  /**
   * Returns a long representing the number of milliseconds this connection has been idle (ie. not data sent or
   * received)
   */
  public long getIdleTime();

  /**
   * Returns a long representing the number of milliseconds since last data received on this connection.
   */
  public long getIdleReceiveTime();

  /**
   * Add the given connection event listener. Re-adding an existing listener will have no effect (ie. the listener will
   * not be in the list twice).
   * 
   * @param listener listener to add
   */
  public boolean addListener(TCConnectionEventListener listener);

  /**
   * Remove the given event listener. Attempting to remove a listener that is not currently in the listeners set has not
   * effect
   */
  public boolean removeListener(TCConnectionEventListener listener);

  /**
   * Close this connection. The actual close happens asynchronously to this call.
   */
  public Future<Void> asynchClose();

  /**
   * Close this connection, blocking for at most the given timeout value
   * 
   * @return true/false whether the connection closed in time
   */
  public void close();

  /**
   * Connect synchronously to a given destination. If this call fails, connect called be called again. However once a
   * connection has successfully connected once, it cannot be re-connected
   * 
   * @param addr the destination address
   * @param timeout time in milliseconds to wait before throwing a timeout exception
   * @throws IOException if there is an error connecting to the destination address
   * @throws TCTimeoutException if a timeout occurs
   */
  public Socket connect(InetSocketAddress addr, int timeout) throws IOException, TCTimeoutException;

  /**
   * Connect asynchronously to the given destination address
   * 
   * @param addr the destination address to connect to
   * @return true if the connection was opened immediately, otherwise false (NOTE: return value of "false" this NOT an
   *         indication of error)
   * @throws IOException if there is an error connecting to the destination
   */
  public boolean asynchConnect(InetSocketAddress addr) throws IOException;

  /**
   * Whether or not this connection is connected
   * 
   * @return true iff this connection is connected to the destination address
   */
  public boolean isConnected();

  /**
   * Whether or not this connection is closed
   * 
   * @return true iff this connection is closed
   */
  public boolean isClosed();

  /**
   * Get local side connection details
   * 
   * @throws IllegalStateException if connection has never been connected
   */
  public InetSocketAddress getLocalAddress();

  /**
   * Get remote side connection details
   * 
   * @throws IllegalStateException if connection has never been connected
   */
  public InetSocketAddress getRemoteAddress();

  /**
   * After TC Transport handshake, a connection is said to be Transport Established
   */
  public void setTransportEstablished();

  public boolean isTransportEstablished();
  
  Map<String, ?> getState();

  default TCByteBufferOutputStream createOutput() {
    return new TCByteBufferOutputStream();
  }
}
