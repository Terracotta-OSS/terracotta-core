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
package org.terracotta.testing.rules;

import java.io.File;
import java.io.IOException;
import org.junit.ClassRule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;

/**
 *
 * @author cdennis
 */
public class BasicExternalClusterActivePassiveIT {

  @ClassRule
  public static final Cluster CLUSTER = new BasicExternalCluster(new File("target/cluster"), 3, 30);

  @Test
  public void testFailover() throws IOException, ConnectionException, Exception {
    CLUSTER.getClusterControl().terminateActive();
    CLUSTER.getClusterControl().startOneServer();
    CLUSTER.getClusterControl().waitForActive();
    
    Connection connection = CLUSTER.newConnection();
    try {
      //do nothing
    } finally {
      connection.close();
    }
  }

  @Test
  public void testFailoverWithLiveConnection() throws IOException, ConnectionException, Exception {
    Connection connection = CLUSTER.newConnection();
    try {
      CLUSTER.getClusterControl().terminateActive();
      CLUSTER.getClusterControl().startOneServer();
      CLUSTER.getClusterControl().waitForActive();
    } finally {
      connection.close();
    }
  }
}
