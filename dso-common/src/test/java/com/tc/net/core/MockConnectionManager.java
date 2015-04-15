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

  @Override
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

  @Override
  public TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory) {
    return this.listener;
  }

  /**
   *
   */

  @Override
  public TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory, int backlog, boolean reuseAddr) {
    return this.listener;
  }

  /**
   *
   */

  @Override
  public void asynchCloseAllConnections() {
    throw new ImplementMe();
  }

  /**
   *
   */

  @Override
  public void closeAllListeners() {
    throw new ImplementMe();
  }

  /**
   *
   */

  @Override
  public void shutdown() {
    throw new ImplementMe();
  }

  @Override
  public TCConnection[] getAllConnections() {
    throw new ImplementMe();
  }

  @Override
  public TCListener[] getAllListeners() {
    throw new ImplementMe();
  }

  @Override
  public void closeAllConnections(long timeout) {
    throw new ImplementMe();
    
  }

  public void createWorkerCommThreads(int count) {
    throw new ImplementMe();
    
  }

  @Override
  public TCComm getTcComm() {
    throw new ImplementMe();
  }

  @Override
  public TCConnection[] getAllActiveConnections() {
    throw new ImplementMe();
  }

}
