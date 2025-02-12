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
package com.tc.object;

import com.tc.net.core.SocketEndpointFactory;
import com.tc.net.core.SocketEndpointFactorySupplier;
import com.tc.net.core.ClearTextSocketEndpointFactory;
import com.tc.net.core.DefaultSocketEndpointFactory;
import com.tc.net.core.ProductID;
import com.terracotta.diagnostic.DiagnosticClientBuilder;

import java.util.Properties;
import org.terracotta.connection.ConnectionPropertyNames;

public class StandardClientBuilderFactory implements ClientBuilderFactory {

  private final String scheme;
  private final SocketEndpointFactorySupplier supplier;

  public StandardClientBuilderFactory(String scheme) {
    DefaultSocketEndpointFactory.setSocketEndpointFactory(new ClearTextSocketEndpointFactory());
    SocketEndpointFactorySupplier base = ClientBuilderFactory.get(SocketEndpointFactorySupplier.class);
    this.scheme = scheme;
    if (base == null) {
      supplier = (p)->new ClearTextSocketEndpointFactory();
    } else {
       supplier = p -> {
        SocketEndpointFactory factory = base.createSocketEndpointFactory(p);
        if (factory == null) {
          return new ClearTextSocketEndpointFactory();
        }
        return factory;
      };
    }
  }
  
  @Override
  public ClientBuilder create(Properties connectionProperties) {
    String type = connectionProperties.getProperty(ConnectionPropertyNames.CONNECTION_TYPE, scheme);
    if (type.equalsIgnoreCase("diagnostic")) {
      return new DiagnosticClientBuilder(connectionProperties, supplier.createSocketEndpointFactory(connectionProperties));
    } else if (type.equalsIgnoreCase("terracotta")) {
      return new StandardClientBuilder(connectionProperties, supplier.createSocketEndpointFactory(connectionProperties));
    } else {
      for (ProductID pid : ProductID.values()) {
        if (pid.name().equalsIgnoreCase(type)) {
          return new StandardClientBuilder(connectionProperties, supplier.createSocketEndpointFactory(connectionProperties));
        }
      }
    }
    throw new IllegalArgumentException(type + " is not a valid connection type");
  }
}
