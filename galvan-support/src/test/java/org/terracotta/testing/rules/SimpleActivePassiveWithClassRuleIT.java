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
import java.util.Collections;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.passthrough.IClusterControl;


/**
 * This test is similar to BasicExternalClusterClassRuleIT, in that it uses the class rule to perform a basic test.
 * 
 * In this case, the basic test is watching how Galvan handles interaction with a basic active-passive cluster.
 */
// XXX: Currently ignored since this test depends on restartability of the server, which now requires a persistence service
//  to be plugged in (and there isn't one available, in open source).
@Ignore
public class SimpleActivePassiveWithClassRuleIT {
  @ClassRule
  public static final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(2)
//          .logConfigExtensionResourceName("custom-logback-ext.xml")
          .build();

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
