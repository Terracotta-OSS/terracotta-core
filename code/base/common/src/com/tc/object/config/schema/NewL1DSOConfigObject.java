/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.BaseNewConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.terracottatech.config.DsoClientData;

public class NewL1DSOConfigObject extends BaseNewConfigObject implements NewL1DSOConfig {

  public static final String                     DSO_INSTRUMENTATION_LOGGING_OPTIONS_SUB_XPATH = "";

  private final IntConfigItem                    faultCount;

  private final DSOInstrumentationLoggingOptions instrumentationLoggingOptions;
  private final DSORuntimeLoggingOptions         runtimeLoggingOptions;
  private final DSORuntimeOutputOptions          runtimeOutputOptions;

  public NewL1DSOConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(DsoClientData.class);

    this.faultCount = this.context.intItem("fault-count");
    this.instrumentationLoggingOptions = new StandardDSOInstrumentationLoggingOptions(this.context);
    this.runtimeLoggingOptions = new StandardDSORuntimeLoggingOptions(this.context);
    this.runtimeOutputOptions = new StandardDSORuntimeOutputOptions(this.context);
  }

  public DSOInstrumentationLoggingOptions instrumentationLoggingOptions() {
    return this.instrumentationLoggingOptions;
  }

  public DSORuntimeLoggingOptions runtimeLoggingOptions() {
    return this.runtimeLoggingOptions;
  }

  public DSORuntimeOutputOptions runtimeOutputOptions() {
    return this.runtimeOutputOptions;
  }

  public IntConfigItem faultCount() {
    return faultCount;
  }

}
