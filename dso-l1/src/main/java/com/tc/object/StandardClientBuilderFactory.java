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
 */
package com.tc.object;

import com.tc.net.core.BufferManagerFactorySupplier;
import com.tc.net.core.ClearTextBufferManagerFactory;
import com.terracotta.diagnostic.DiagnosticClientBuilder;

import java.util.Arrays;
import java.util.Properties;

public class StandardClientBuilderFactory implements ClientBuilderFactory {

  private final BufferManagerFactorySupplier supplier;

  public StandardClientBuilderFactory() {
    BufferManagerFactorySupplier s = ClientBuilderFactory.get(BufferManagerFactorySupplier.class);
    if (s == null) {
      s = (p)->new ClearTextBufferManagerFactory();
    }
    supplier = s;
  }
  
  @Override
  public ClientBuilder create(Properties connectionProperties) {
    Object clientBuilderTypeValue = connectionProperties.get(CLIENT_BUILDER_TYPE);
    if (clientBuilderTypeValue instanceof ClientBuilderType) {
      ClientBuilderType connectionType = (ClientBuilderType)clientBuilderTypeValue;
      if (connectionType == ClientBuilderType.TERRACOTTA) {
        return new StandardClientBuilder(connectionProperties, supplier.createBufferManagerFactory(connectionProperties));
      } else if (connectionType == ClientBuilderType.DIAGNOSTIC) {
        return new DiagnosticClientBuilder(connectionProperties, supplier.createBufferManagerFactory(connectionProperties));
      } else {
        throw new IllegalArgumentException(connectionType + " is not a valid client builder type, valid client " +
                                           "builder types " + Arrays.toString(ClientBuilderType.values()));
      }
    } else {
      throw new IllegalArgumentException("Received invalid value (" + clientBuilderTypeValue + ") for property "
                                         + CLIENT_BUILDER_TYPE + ", valid client builder types " +
                                         Arrays.toString(ClientBuilderType.values()));
    }
  }
}
