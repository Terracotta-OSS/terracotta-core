/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

public class HaConfigBuilder extends BaseConfigBuilder {
  public static final String    HA_MODE_NETWORKED_ACTIVE_PASSIVE       = "networked-active-passive";
  public static final String    HA_MODE_ACTIVE_PASSIVE                 = "active-passive";

  private static final String   MODE                                   = "mode";
  private static final String   NETWORKED_ACTIVE_PASSIVE_ELECTION_TIME = "election-time";
  private static final String[] NETWORKED_ACTIVE_PASSIVE               = concat(new Object[] { NETWORKED_ACTIVE_PASSIVE_ELECTION_TIME });
  private static final String[] ALL_PROPERTIES                         = concat(new Object[] { MODE,
      NETWORKED_ACTIVE_PASSIVE                                        });

  public HaConfigBuilder() {
    super(3, ALL_PROPERTIES);
  }

  public void setMode(String data) {
    setProperty("mode", data);
  }

  public void setElectionTime(String data) {
    setProperty("election-time", data);
  }

  public String toString() {
    String out = "";

    out += openElement("ha");

    out += element(MODE);

    String networkedActivePassiveString = openElement("networked-active-passive", NETWORKED_ACTIVE_PASSIVE);
    if (!networkedActivePassiveString.equals("")) {
      out += networkedActivePassiveString + element(NETWORKED_ACTIVE_PASSIVE_ELECTION_TIME)
          + closeElement("networked-active-passive");
    }

    out += closeElement("ha");

    return out;
  }

  public static HaConfigBuilder newMinimalInstance() {
    return new HaConfigBuilder();
  }

}
