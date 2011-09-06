/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.config.schema.HaConfigSchema;

public class StateManagerConfigImpl implements StateManagerConfig {

  private final HaConfigSchema haConfig;

  public StateManagerConfigImpl(HaConfigSchema haConfig) {
    this.haConfig = haConfig;
  }

  public int getElectionTimeInSecs() {
    int electionTime = -1;

    if (haConfig.isNetworkedActivePassive()) {
      electionTime = haConfig.getHa().getNetworkedActivePassive().getElectionTime();
    } else {
      throw new AssertionError("Networked Active Passive is not enabled in config");
    }

    if (electionTime <= 0) { throw new AssertionError("Election time has to be a positive integer, but is set to "
                                                      + electionTime + " secs. in config"); }

    return electionTime;
  }

}
