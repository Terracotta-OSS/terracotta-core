/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.config.schema.NewHaConfig;

public class StateManagerConfigImpl implements StateManagerConfig {

  private final NewHaConfig haConfig;

  public StateManagerConfigImpl(NewHaConfig haConfig) {
    this.haConfig = haConfig;
  }

  public int getElectionTimeInSecs() {
    int electionTime = -1;

    if (haConfig.isNetworkedActivePassive()) {
      electionTime = haConfig.electionTime().getInt();
    }

    if (electionTime < 0) { throw new AssertionError("Election time was not set."); }

    return electionTime;
  }

}
