/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.object.DistributedObjectClient;
import com.tc.server.TCServerImpl;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.util.PortChooser;

public class L1SRACorrectnessTest extends AbstractAgentSRACorrectnessTestCase {

  public void testCheckL1SRAs() throws Exception {
    final PortChooser pc = new PortChooser();
    final int dsoPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();
    final TCServerImpl server = (TCServerImpl)startupServer(dsoPort, jmxPort);

    try {
      final TestPauseListener pauseListener = new TestPauseListener();
      final DistributedObjectClient client = startupClient(dsoPort, jmxPort, pauseListener);
      try {
        // wait until client handshake is complete...
        pauseListener.waitUntilUnpaused();

        // verify that all the registered SRA classes are correct
        StatisticsAgentSubSystem agent = client.getStatisticsAgentSubSystem();
        checkSRAsInRegistry(agent);
      } finally {
        client.getCommunicationsManager().shutdown();
        client.stop();
      }
    } finally {
      server.stop();
    }
  }
}