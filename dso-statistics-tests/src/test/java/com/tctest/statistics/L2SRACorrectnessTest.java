/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.tc.server.TCServerImpl;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.util.PortChooser;

public class L2SRACorrectnessTest extends AbstractAgentSRACorrectnessTestCase {
  public void testCheckL2SRAs() throws Exception {
    final PortChooser pc = new PortChooser();
    final int dsoPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();
    final int l2GroupPort = pc.chooseRandomPort();
    final TCServerImpl server = (TCServerImpl) startupServer(dsoPort, jmxPort, l2GroupPort);

    try {
      // verify that all the registered SRA classes are correct
      StatisticsAgentSubSystem agent = server.getDSOServer().getStatisticsAgentSubSystem();
      checkSRAsInRegistry(agent);
    } finally {
      server.stop();
    }
  }
}