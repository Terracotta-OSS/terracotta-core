/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.terracotta.diagnostic;


import com.tc.net.basic.BasicConnectionManager;
import com.tc.net.core.BufferManagerFactory;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.object.ClientEntityManager;
import com.tc.object.StandardClientBuilder;
import com.tc.net.core.ProductID;

import java.util.Properties;


public class DiagnosticClientBuilder extends StandardClientBuilder {

  public DiagnosticClientBuilder(Properties connectionProperties, BufferManagerFactory buffers) {
    super(connectionProperties, buffers);
  }
  
  @Override
  public ClientEntityManager createClientEntityManager(ClientMessageChannel channel) {
    return new DiagnosticClientEntityManager(channel);
  }

  @Override
  public ProductID getTypeOfClient() {
    return ProductID.DIAGNOSTIC;
  }

  @Override
  public TCConnectionManager createConnectionManager(String uuid, String name) {
    return new BasicConnectionManager(name + "/" + uuid, getBufferManagerFactory());
  }
}
