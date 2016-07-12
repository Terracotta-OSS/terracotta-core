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
 * A test to ensure that a cluster can be installed in a directory with problematic characters in its path ("("/")").
 * This is to point out an issue with how a command invocation can fail, on Windows, if the path has strange
 * characters.
 * 
 * The expected behavior is that the cluster will start, a connection will be successfully established, and it can
 * be closed.  If there were a problem with the server start-up, the connection would fail.
 */
public class BasicExternalClusterBadPathIT {
  @ClassRule
  public static final Cluster cluster = new BasicExternalCluster(new File("target/(build)/cluster"), 1);

  @Test
  public void parenthesisInPathTest() throws IOException, ConnectionException
  {
    Connection connection = cluster.newConnection();
    try {
      //do nothing
    } finally {
      connection.close();
    }
  }
}
