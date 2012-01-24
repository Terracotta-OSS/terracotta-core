/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.BaseConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.terracottatech.config.DsoClientData;
import com.terracottatech.config.RuntimeOutputOptions;

/**
 * The standard implementation of {@link DSORuntimeOutputOptions}.
 */
public class StandardDSORuntimeOutputOptions extends BaseConfigObject implements DSORuntimeOutputOptions {

  private final boolean doAutoLockDetails;
  private final boolean doCaller;
  private final boolean doFullStack;

  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();
  private static final TCLogger logger = TCLogging.getLogger(StandardDSORuntimeOutputOptions.class);

  public StandardDSORuntimeOutputOptions(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(DsoClientData.class);
    DsoClientData dsoClientData = (DsoClientData)this.context.bean();
    RuntimeOutputOptions runtimeOutputOptions = dsoClientData.getDebugging().getRuntimeOutputOptions();

    this.doAutoLockDetails = runtimeOutputOptions.getAutoLockDetails();
    this.doCaller = runtimeOutputOptions.getCaller();
    this.doFullStack = runtimeOutputOptions.getFullStack();

    if (doCaller) {
      //CDV-731: deprecate use of <caller> in <runtime-output-options>
      final String msg = "<caller> element in <runtime-output-options> is deprecated. " +
                         "Please set the value of <caller> to false in your tc-config file to remove this warning.";
      consoleLogger.warn(msg);
      logger.warn(msg);
    }
  }

  public boolean doAutoLockDetails() {
    return this.doAutoLockDetails;
  }

  public boolean doCaller() {
    return this.doCaller;
  }

  public boolean doFullStack() {
    return this.doFullStack;
  }

}
