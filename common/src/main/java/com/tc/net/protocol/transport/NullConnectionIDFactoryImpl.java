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
package com.tc.net.protocol.transport;

import com.tc.net.ClientID;
import com.tc.net.StripeID;
import com.tc.util.Assert;
import java.util.concurrent.atomic.AtomicLong;

public class NullConnectionIDFactoryImpl implements ConnectionIDFactory {
  
  private final AtomicLong cid = new AtomicLong(-2);

  public NullConnectionIDFactoryImpl() {
    
  }

  @Override
  public long getCurrentConnectionID() {
    return cid.get();
  }

  @Override
  public ConnectionID populateConnectionID(ConnectionID connectionID) {
// only assign invalid IDs as these connections are not real
    ConnectionID connection = new ConnectionID(connectionID.getJvmID(), cid.decrementAndGet(), null, null, connectionID.getProductId());
    Assert.assertTrue(!connection.isValid());
    return connection;
  }

  @Override
  public void restoreConnectionId(ConnectionID rv) {

  }

  @Override
  public void registerForConnectionIDEvents(ConnectionIDFactoryListener listener) {

  }

  @Override
  public void activate(StripeID stripeID, long nextAvailChannelID) {
    
  }

  @Override
  public ConnectionID buildConnectionID(ClientID client) {
    throw new AssertionError();
  }
}