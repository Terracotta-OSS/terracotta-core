/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
  // TODO: There should be a way to enumerate existing listeners and connections

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
   * Get all active listener instances created by this manager
   */
  public TCListener[] getAllListeners();
  
}