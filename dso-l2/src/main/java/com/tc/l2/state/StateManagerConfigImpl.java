/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

public class StateManagerConfigImpl implements StateManagerConfig {

  private final int electionTimeInSecs;

  public StateManagerConfigImpl(int electionTimeInSecs) {
    this.electionTimeInSecs = electionTimeInSecs;

    if (electionTimeInSecs <= 0) { throw new AssertionError(
                                                            "Election time has to be a positive integer, but is set to "
                                                                + electionTimeInSecs + " secs. in config"); }
  }

  @Override
  public int getElectionTimeInSecs() {
    return electionTimeInSecs;

  }

}
