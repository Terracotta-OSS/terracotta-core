/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.testing.rules;

import java.io.File;
import java.util.Collections;

import org.junit.ClassRule;
import org.junit.Test;
import org.terracotta.passthrough.IClusterControl;


/**
 * This test is similar to BasicExternalClusterClassRuleIT, in that it uses the class rule to perform a basic test.
 * 
 * In this case, the basic test is watching how Galvan handles interaction with a basic active-passive cluster.
 */
public class SimpleActivePassiveWithClassRuleIT {
  @ClassRule
  public static final Cluster CLUSTER = new BasicExternalCluster(new File("target/cluster"), 2, Collections.<File>emptyList(), "", "", "", true);

  /**
   * This will ensure that a fail-over correctly happens.
   */
  @Test
  public void testRestartActive() throws Exception {
    IClusterControl control = CLUSTER.getClusterControl();
    
    // Wait for everything to start - should be redundant, but just to make the test clear.
    control.waitForActive();
    control.waitForRunningPassivesInStandby();
    
    // Terminate the active.
    control.terminateActive();
    // Wait for the passive to take over.
    control.waitForActive();
    
    // Restart the terminated active.
    control.startOneServer();
    // Make sure that the passive comes up.
    control.waitForRunningPassivesInStandby();
  }

  /**
   * This will ensure that a ZAP correctly happens.
   */
  @Test
  public void testRestartPassive() throws Exception {
    IClusterControl control = CLUSTER.getClusterControl();
    
    // Wait for everything to start - should be redundant, but just to make the test clear.
    control.waitForActive();
    control.waitForRunningPassivesInStandby();
    
    // Terminate the passive.
    control.terminateOnePassive();
    
    // Restart the terminated server.
    control.startOneServer();
    // Make sure that the passive comes up (we expect that this will cause the server to come up, get zapped, and then restart as passive).
    control.waitForRunningPassivesInStandby();
  }
}
