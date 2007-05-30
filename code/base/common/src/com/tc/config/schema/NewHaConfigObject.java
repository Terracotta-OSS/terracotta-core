/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;
import com.terracottatech.config.Ha;
import com.terracottatech.config.HaMode;

public class NewHaConfigObject extends BaseNewConfigObject implements NewHaConfig {
  private final StringConfigItem haMode;
  private final IntConfigItem    electionTime;

  public NewHaConfigObject(ConfigContext context) {
    super(context);

    context.ensureRepositoryProvides(Ha.class);

    haMode = context.stringItem("mode");
    electionTime = context.intItem("networked-active-passive/election-time");
  }

  public StringConfigItem haMode() {
    return haMode;
  }

  public IntConfigItem electionTime() {
    return electionTime;
  }

  public boolean isNetworked() {
    return haMode.getString().equals(HaMode.NETWORKED_ACTIVE_PASSIVE.toString());
  }

  public boolean isNetworkedActivePassive() {
    return haMode.getString().equals(HaMode.NETWORKED_ACTIVE_PASSIVE.toString());
  }
}
