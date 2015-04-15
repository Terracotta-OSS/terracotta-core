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

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;

import java.io.IOException;

/**
 * Manages connections and listeners. The connection manager also provides default implementations of connection event
 * handlers. Specifically, the default behaviour is close connections upon any error event
 * 
 * @author teck
 */
public interface TCConnectionManager {

  /**
   * Create a new non-connected connection
   * 
   * @param adaptor protocol adaptor to use for incoming network data
   */
  public TCConnection createConnection(TCProtocolAdaptor adaptor);

  /**
   * Create a new listening socket (ie. java.net.ServerSocket) on the given socket address. A default accept queue depth
   * will be used, and reuseAddress will be true
   * 
   * @param addr the address to bind the listener to
   * @param factory protocol adaptor factory used to attach protocol adaptors to newly accpted connections
   */
  public TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory) throws IOException;

  /**
   * Create a new listening socket (ie. java.net.ServerSocket) on the given socket address with the given accect queue
   * depth
   * 
   * @param addr the address to bind the listener to
   * @param factory protocol adaptor factory used to attach protocol adaptors to newly accpted connections
   * @param backlog accept queue backlog depth
   * @param reuseAddr whether the bind port will be reused if in use by open sockets
   */
  public TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory, int backlog, boolean reuseAddr)
      throws IOException;

  /**
   * Close any open connections created through this connection manager instance
   */
  public void asynchCloseAllConnections();

  public void closeAllConnections(long timeout);

  /**
   * Close all listeners created through this connection manager instance
   */
  public void closeAllListeners();

  /**
   * Shutdown this connection manager. Shutdown will call <code>closeAllConnections()</code> and
   * <code>closeAllListeners()</code> on your behalf. A shutdown connection manager can be reused or restarted
   */
  public void shutdown();

  /**
   * Get all non-closed connection instances created by this manager
   */
  public TCConnection[] getAllConnections();

  /**
   * Get all healthy connection instances created by this manager.
   */
  public TCConnection[] getAllActiveConnections();

  /**
   * Get all active listener instances created by this manager
   */
  public TCListener[] getAllListeners();

  /**
   * Get the associated comm implementation cotext -- used for testing only
   */
  public TCComm getTcComm();
}
