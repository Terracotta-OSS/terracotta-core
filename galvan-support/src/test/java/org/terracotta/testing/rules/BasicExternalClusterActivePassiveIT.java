/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.testing.rules;

import java.io.File;
import java.io.IOException;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;

/**
 *
 * @author cdennis
 */
@Ignore
public class BasicExternalClusterActivePassiveIT {

  @ClassRule
  public static final Cluster CLUSTER = new BasicExternalCluster(new File("target/cluster"), 3);

  @Test
  public void testFailover() throws IOException, ConnectionException, Exception {
    CLUSTER.getClusterControl().restartActive();
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
      CLUSTER.getClusterControl().restartActive();
      CLUSTER.getClusterControl().waitForActive();
    } finally {
      connection.close();
    }
  }
}
