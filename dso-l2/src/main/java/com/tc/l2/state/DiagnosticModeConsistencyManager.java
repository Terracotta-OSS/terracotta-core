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
package com.tc.l2.state;

import com.tc.logging.TCLogging;
import com.tc.net.NodeID;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

public class DiagnosticModeConsistencyManager implements ConsistencyManager {
  @Override
  public boolean requestTransition(ServerMode mode, NodeID sourceNode, Transition newMode) throws IllegalStateException {

    TCLogging.getConsoleLogger().info("Started the server in diagnostic mode");

    // to avoid further elections and transition requests
    waitForEver();

    return false;
  }

  @Override
  public boolean lastTransitionSuspended() {
    return false;
  }

  @Override
  public void allowLastTransition() {

  }

  @Override
  public Collection<Transition> requestedActions() {
    return null;
  }

  @Override
  public long getCurrentTerm() {
    return 0;
  }

  private static void waitForEver() {
    try {
      new CountDownLatch(1).await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
