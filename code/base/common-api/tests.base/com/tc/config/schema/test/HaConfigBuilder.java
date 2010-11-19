/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

public class HaConfigBuilder extends BaseConfigBuilder {
  public static final String    HA_MODE_NETWORKED_ACTIVE_PASSIVE       = "networked-active-passive";
  public static final String    HA_MODE_DISK_BASED_ACTIVE_PASSIVE      = "disk-based-active-passive";

  private static final String   MODE                                   = "mode";
  private static final String   NETWORKED_ACTIVE_PASSIVE_ELECTION_TIME = "election-time";
  private static final String[] NETWORKED_ACTIVE_PASSIVE               = concat(new Object[] { NETWORKED_ACTIVE_PASSIVE_ELECTION_TIME });
  private static final String[] ALL_PROPERTIES                         = concat(new Object[] { MODE,
      NETWORKED_ACTIVE_PASSIVE                                        });

  private String                haMode;
  private String                electionTime;

  public HaConfigBuilder() {
    super(7, ALL_PROPERTIES);
  }

  public HaConfigBuilder(int indent) {
    super(indent, ALL_PROPERTIES);
  }

  public void setMode(String data) {
    setProperty("mode", data);
    this.haMode = data;
  }

  public String getMode() {
    return this.haMode;
  }

  public void setElectionTime(String data) {
    setProperty("election-time", data);
    this.electionTime = data;
  }

  public String getElectionTime() {
    return this.electionTime;
  }

  @Override
  public String toString() {
    String out = "";

    out += openElement("ha");

    out += element(MODE);

    if (this.haMode.equals(HA_MODE_NETWORKED_ACTIVE_PASSIVE)) {
      String networkedActivePassiveString = openElement("networked-active-passive", NETWORKED_ACTIVE_PASSIVE);
      if (!networkedActivePassiveString.equals("")) {
        out += networkedActivePassiveString + element(NETWORKED_ACTIVE_PASSIVE_ELECTION_TIME)
               + closeElement("networked-active-passive");
      }
    }

    out += closeElement("ha");

    return out;
  }

  public static HaConfigBuilder newMinimalInstance() {
    return new HaConfigBuilder();
  }

}
