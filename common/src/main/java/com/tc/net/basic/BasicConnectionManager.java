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
package com.tc.net.basic;

import com.tc.net.core.SocketEndpointFactory;
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
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 *
 */
public class BasicConnectionManager implements TCConnectionManager {
  private final Set<TCConnection>       connections            = new HashSet<>();
  private final SocketEndpointFactory buffers;
  private final String id;

  public BasicConnectionManager(String id, SocketEndpointFactory buffers) {
    this.buffers = buffers;
    this.id = id;
  }

  @Override
  public TCConnection createConnection(TCProtocolAdaptor adaptor) {
    synchronized (connections) {
      TCConnection basic = new BasicConnection(id, adaptor, buffers, (conn)->{
        synchronized (connections) {
          connections.remove(conn);
        }
      });
      connections.add(basic);
      return basic;
    }
  }

  @Override
  public TCListener createListener(InetSocketAddress addr, ProtocolAdaptorFactory factory) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public TCListener createListener(InetSocketAddress addr, ProtocolAdaptorFactory factory, int backlog, boolean reuseAddr) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void closeAllConnections() {
    Arrays.asList(getAllConnections()).forEach(c->c.close());
  }

  @Override
  public void asynchCloseAllConnections() {
    Arrays.asList(getAllConnections()).forEach(c->c.asynchClose());
  }
  
  @Override
  public void closeAllListeners() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void shutdown() {
    asynchCloseAllConnections();
  }

  @Override
  public synchronized TCConnection[] getAllConnections() {
    TCConnection[] all = new TCConnection[connections.size()];
    return connections.toArray(all);
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
  
  @Override
  public int getBufferCount() {
    return 0;
  }
}
