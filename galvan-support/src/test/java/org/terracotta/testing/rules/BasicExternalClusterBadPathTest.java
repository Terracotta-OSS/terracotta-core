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
import java.util.Collections;
import org.junit.ClassRule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;

/**
 *
 * @author GGIB
 */
public class BasicExternalClusterBadPathTest {

  private static final String RESOURCE_CONFIG =
      "<service xmlns:ohr='http://www.terracotta.org/config/offheap-resource' id=\"resources\">"
          + "<ohr:offheap-resources>"
          + "<ohr:resource name=\"primary-server-resource\" unit=\"MB\">64</ohr:resource>"
          + "</ohr:offheap-resources>" +
          "</service>\n";

  @ClassRule
  private final Cluster cluster =
      new BasicExternalCluster(new File("(build)/cluster"), 1, Collections.<File>emptyList(), "", RESOURCE_CONFIG, null);

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
