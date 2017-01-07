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
package com.tc.objectserver.impl;

import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionIDFactoryListener;
import java.util.Collections;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class NullConnectionIDFactoryImpl implements ConnectionIDFactory {
  
  private final AtomicLong cid = new AtomicLong();

  public NullConnectionIDFactoryImpl() {
    
  }

  @Override
  public long getCurrentConnectionID() {
    return cid.get();
  }

  @Override
  public ConnectionID populateConnectionID(ConnectionID connectionID) {
    return new ConnectionID(connectionID.getJvmID(), cid.incrementAndGet());
  }

  @Override
  public void restoreConnectionId(ConnectionID rv) {

  }

  @Override
  public Set<ConnectionID> loadConnectionIDs() {
    return Collections.emptySet();
  }

  @Override
  public void registerForConnectionIDEvents(ConnectionIDFactoryListener listener) {

  }

  @Override
  public void init(String clusterID, long nextAvailChannelID, Set<ConnectionID> connections) {

  }
}