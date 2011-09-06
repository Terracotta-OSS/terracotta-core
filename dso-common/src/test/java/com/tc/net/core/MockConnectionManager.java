/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.exception.ImplementMe;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;

/**
 * TODO Jan 4, 2005: comment describing what this class is for.
 */
public class MockConnectionManager implements TCConnectionManager {

  TCConnection conn;
  
  TCListener listener = new TestTCListener();
  
  int createConnectionCallCount = 0;
  
  public void setConnection(TCConnection conn) {
    this.conn = conn;
  }
  
  public void setListener(TCListener listener) {
    this.listener = listener;
  }
  
  public int getCreateConnectionCallCount() {
    return createConnectionCallCount;
  }
  
  /**
   *
   */

  public TCConnection createConnection(TCProtocolAdaptor adaptor) {
    createConnectionCallCount++;
    return conn;
  }

  /**
   *
   */

  public TCConnection createConnection(TCProtocolAdaptor adaptor, TCConnectionEventListener lsnr) {
    createConnectionCallCount++;
    return conn;
  }

  /**
   *
   */

  public TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory) {
    return this.listener;
  }

  /**
   *
   */

  public TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory, int backlog, boolean reuseAddr) {
    return this.listener;
  }

  /**
   *
   */

  public void asynchCloseAllConnections() {
    throw new ImplementMe();
  }

  /**
   *
   */

  public void closeAllListeners() {
    throw new ImplementMe();
  }

  /**
   *
   */

  public void shutdown() {
    throw new ImplementMe();
  }

  public TCConnection[] getAllConnections() {
    throw new ImplementMe();
  }

  public TCListener[] getAllListeners() {
    throw new ImplementMe();
  }

  public void closeAllConnections(long timeout) {
    throw new ImplementMe();
    
  }

  public void createWorkerCommThreads(int count) {
    throw new ImplementMe();
    
  }

  public TCComm getTcComm() {
    throw new ImplementMe();
  }

  public TCConnection[] getAllActiveConnections() {
    throw new ImplementMe();
  }

}
