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
 *
 */
package org.terracotta.testing.rules;

import java.util.Properties;

import org.junit.ClassRule;
import org.junit.Ignore;
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
  public static final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster().withServiceFragment("Bogus<Frag").build();

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
