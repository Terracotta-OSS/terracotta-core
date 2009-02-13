/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.BaseNewConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.terracottatech.config.DsoClientData;

/**
 * The standard implementation of {@link DSORuntimeOutputOptions}.
 */
public class StandardDSORuntimeOutputOptions extends BaseNewConfigObject implements DSORuntimeOutputOptions {

  private final BooleanConfigItem doAutoLockDetails;
  private final BooleanConfigItem doCaller;
  private final BooleanConfigItem doFullStack;

  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();
  private static final TCLogger logger = TCLogging.getLogger(StandardDSORuntimeOutputOptions.class);

  public StandardDSORuntimeOutputOptions(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(DsoClientData.class);

    this.doAutoLockDetails = this.context.booleanItem("debugging/runtime-output-options/auto-lock-details");
    this.doCaller = this.context.booleanItem("debugging/runtime-output-options/caller");
    this.doFullStack = this.context.booleanItem("debugging/runtime-output-options/full-stack");

    if (doCaller.getBoolean()) {
      //CDV-731: deprecate use of <caller> in <runtime-output-options>
      final String msg = "<caller> element in <runtime-output-options> is deprecated. " +
                         "Please set the value of <caller> to false in your tc-config file to remove this warning.";
      consoleLogger.warn(msg);
      logger.warn(msg);
    }
  }

  public BooleanConfigItem doAutoLockDetails() {
    return this.doAutoLockDetails;
  }

  public BooleanConfigItem doCaller() {
    return this.doCaller;
  }

  public BooleanConfigItem doFullStack() {
    return this.doFullStack;
  }

}
