/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.terracottatech.config.UpdateCheck;

public class UpdateCheckConfigObject extends BaseNewConfigObject implements UpdateCheckConfig {
  private final BooleanConfigItem isEnabled;
  private final IntConfigItem     periodDays;

  public UpdateCheckConfigObject(ConfigContext context) {
    super(context);

    context.ensureRepositoryProvides(UpdateCheck.class);

    isEnabled = context.booleanItem("enabled");
    periodDays = context.intItem("period-days");
  }

  public BooleanConfigItem isEnabled() {
    return isEnabled;
  }

  public IntConfigItem periodDays() {
    return periodDays;
  }
}
