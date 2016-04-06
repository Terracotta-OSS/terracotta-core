/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.testing.rules;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;

/**
 *
 * @author cdennis
 */
public class BasicExternalClusterMethodRuleIT {

  @Rule
  public final Cluster cluster = new BasicExternalCluster(new File("target/cluster"), 1);

  @Test
  public void testDirectConnection() throws IOException, ConnectionException {
    Connection connection = cluster.newConnection();
    try {
      //do nothing
    } finally {
      connection.close();
    }
  }

  @Test
  public void testConnectionViaURI() throws IOException, ConnectionException {
    Connection connection = ConnectionFactory.connect(cluster.getConnectionURI(), new Properties());
    try {
      //do nothing
    } finally {
      connection.close();
    }
  }
}
