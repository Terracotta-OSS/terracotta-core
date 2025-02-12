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
package com.tc.l2.ha;

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.util.Assert;

public class ConnectionIDWeightGenerator implements WeightGenerator {
  private final ConnectionIDFactory persistor;

  public ConnectionIDWeightGenerator(ConnectionIDFactory persistor) {
    Assert.assertNotNull(persistor);
    this.persistor = persistor;
  }

  @Override
  public long getWeight() {
    // return connectionID that the server is currently on.  Since this is increasing,
    // the server with more connections over lifetime will win
    return persistor.getCurrentConnectionID();
  }

  @Override
  public boolean isVerificationWeight() {
    return true;
  }
}
