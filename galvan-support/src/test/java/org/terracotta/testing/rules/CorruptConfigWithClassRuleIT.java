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
import java.util.Collections;
import java.util.Properties;

import org.junit.ClassRule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.testing.master.GalvanFailureException;


/**
 * This test is effectly the same as BasicExternalClusterClassRuleIT except that it deliberately corrupts the server config
 * so that the server will fail to start.  It then runs 2 tests on the same class rule to ensure that re-using this broken
 * cluster instance will return failure in both cases.
 */
public class CorruptConfigWithClassRuleIT {
  @ClassRule
  public static final Cluster CLUSTER = new BasicExternalCluster(new File("target/cluster"), 1, Collections.<File>emptyList(), "", "", "BOGUS<ENTITY<FRAGMENT");

  @Test(expected=ConnectionException.class)
  public void testDirectConnection() throws Exception {
    Connection connection = CLUSTER.newConnection();
    try {
      //do nothing
    } finally {
      connection.close();
    }
  }

  // We expect GalvanFailureException when the active fails to come up while we are waiting for it, since this is a kind of
  //  test failure scenario.
  @Test(expected=GalvanFailureException.class)
  public void testWaitForActiveCrash() throws Exception {
    CLUSTER.getClusterControl().waitForActive();
  }

  @Test(expected=ConnectionException.class)
  public void testConnectionViaURI() throws Exception {
    Connection connection = ConnectionFactory.connect(CLUSTER.getConnectionURI(), new Properties());
    try {
      //do nothing
    } finally {
      connection.close();
    }
  }
}
