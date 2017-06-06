/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.support;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.helper.client.HelperEntity;
import org.terracotta.helper.common.HelperEntityConstants;
import org.terracotta.passthrough.IClientTestEnvironment;
import org.terracotta.passthrough.IClusterControl;

import java.net.URI;
import java.util.Properties;


public class SimpleDumpTestIT extends MultiProcessGalvanTest {
  private static final int CLIENT_COUNT = 2;
  
  @Override
  public int getClientsToStart() {
    return CLIENT_COUNT;
  }

  @Override
  public void runSetup(IClientTestEnvironment env, IClusterControl control) {
  }

  @Override
  public void runDestroy(IClientTestEnvironment env, IClusterControl control) {
  }

  @Override
  public void runTest(IClientTestEnvironment env, IClusterControl control) throws Throwable {
    Connection connection = ConnectionFactory.connect(URI.create(env.getClusterUri()), new Properties());
    int clientIndex = env.getThisClientIndex();
    if(clientIndex == 0) {
      EntityRef<HelperEntity, Object, Object> entityRef = connection.getEntityRef(HelperEntity.class, HelperEntityConstants.HELPER_ENTITY_VERSION, HelperEntityConstants.HELPER_ENTITY_NAME);
      HelperEntity helperEntity = entityRef.fetchEntity(null);
      helperEntity.dumpState();
      helperEntity.close();
    }
    Thread.sleep(10 * 1000);
  }
}
