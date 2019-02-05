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
package com.tc.net.basic;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.BufferManagerFactory;
import com.tc.net.core.TCComm;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCListener;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.tc.net.protocol.TCProtocolAdaptor;
import java.util.ArrayList;

/**
 *
 * @author mscott
 */
public class BasicConnectionManager implements TCConnectionManager {
  private final Set<TCConnection>       connections            = new HashSet<>();
  private final BufferManagerFactory buffers;

  public BasicConnectionManager(BufferManagerFactory buffers) {
    this.buffers = buffers;
  }

  @Override
  public TCConnection createConnection(TCProtocolAdaptor adaptor) {
    synchronized (connections) {
      TCConnection basic = new BasicConnection(adaptor, buffers, (conn)->{
        synchronized (connections) {
          connections.remove(conn);
          return null;
        }
      });
      connections.add(basic);
      return basic;
    }
  }

  @Override
  public TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory, int backlog, boolean reuseAddr) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void asynchCloseAllConnections() {
    new Thread(()->closeAllConnections(1000)).start();
  }

  @Override
  public synchronized void closeAllConnections(long timeout) {
    new ArrayList<>(connections).forEach(c->c.close(timeout));
  }

  @Override
  public void closeAllListeners() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void shutdown() {
    closeAllConnections(30000);
  }

  @Override
  public TCConnection[] getAllConnections() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public TCConnection[] getAllActiveConnections() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public TCListener[] getAllListeners() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public TCComm getTcComm() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Map<String, ?> getStateMap() {
    return new LinkedHashMap<>();
  }

  
  
}
