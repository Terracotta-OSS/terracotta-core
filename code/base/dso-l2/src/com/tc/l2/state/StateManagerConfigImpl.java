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
    } else {
      throw new AssertionError("Networked Active Passive is not enabled in config");
    }

    if (electionTime <= 0) { throw new AssertionError("Election time has to be a positive integer, but is set to " + electionTime
                                                      + " secs. in config"); }

    return electionTime;
  }

}
