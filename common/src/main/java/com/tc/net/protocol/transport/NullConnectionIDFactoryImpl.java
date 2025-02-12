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
package com.tc.net.protocol.transport;

import com.tc.net.StripeID;
import com.tc.net.core.ProductID;
import static com.tc.net.core.ProductID.DIAGNOSTIC;
import com.tc.net.protocol.tcm.ChannelID;
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
    ProductID requested = connectionID.getProductId();
    switch (requested) {
      case DIAGNOSTIC:
        break;
      default:
        requested = ProductID.INFORMATIONAL;
    }
    ConnectionID connection = new ConnectionID(connectionID.getJvmID(), cid.decrementAndGet(), requested);
    Assert.assertTrue(connection.getChannelID() < ChannelID.NULL_ID.toLong());
    return connection;
  }

  @Override
  public void registerForConnectionIDEvents(ConnectionIDFactoryListener listener) {

  }

  @Override
  public void activate(StripeID stripeID, long nextAvailChannelID) {
    
  }
}