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
package org.terracotta.testing.rules;

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.connection.ConnectionException;
import org.terracotta.testing.common.Assert;


/**
 * A test that attempts to immediately control the cluster, via IClusterControl, behave correctly.
 * This addresses the issue demonstrated in issue-111.
 */
public class BasicExternalClusterImmediateControlIT {
  private static final int SERVER_COUNT = 2;

  @Rule
  public final Cluster cluster = BasicExternalClusterBuilder.newCluster(SERVER_COUNT).build();

  @Test
  public void waitForActive() throws IOException, ConnectionException {
    try {
      this.cluster.getClusterControl().waitForActive();
    } catch (Exception e) {
      Assert.unexpected(e);
    }
  }

  @Test
  public void waitForRunningPassivesInStandby() throws IOException, ConnectionException {
    try {
      this.cluster.getClusterControl().waitForRunningPassivesInStandby();
    } catch (Exception e) {
      Assert.unexpected(e);
    }
  }

  @Test
  public void startOneServer() throws IOException, ConnectionException {
    boolean didFail = false;
    try {
      this.cluster.getClusterControl().startOneServer();
      didFail = false;
    } catch (IllegalStateException e) {
      // This is expected - there are no terminated servers.
      didFail = true;
    } catch (Exception e) {
      Assert.unexpected(e);
    }
    Assert.assertTrue(didFail);
  }

  @Test
  public void startAllServers() throws IOException, ConnectionException {
    try {
      this.cluster.getClusterControl().startAllServers();
    } catch (Exception e) {
      Assert.unexpected(e);
    }
  }

  @Test
  public void terminateActive() throws IOException, ConnectionException {
    try {
      this.cluster.getClusterControl().terminateActive();
    } catch (Exception e) {
      Assert.unexpected(e);
    }
  }

  @Test
  public void terminateOnePassive() throws IOException, ConnectionException {
    try {
      this.cluster.getClusterControl().terminateOnePassive();
    } catch (Exception e) {
      Assert.unexpected(e);
    }
  }

  @Test
  public void terminateAllServers() throws IOException, ConnectionException {
    try {
      this.cluster.getClusterControl().terminateAllServers();
    } catch (Exception e) {
      Assert.unexpected(e);
    }
  }
}
