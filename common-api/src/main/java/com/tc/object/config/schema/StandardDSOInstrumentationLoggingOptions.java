/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.BaseConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.terracottatech.config.DsoClientData;
import com.terracottatech.config.InstrumentationLogging;

/**
 * The standard implementation of {@link DSOInstrumentationLoggingOptions}.
 */
public class StandardDSOInstrumentationLoggingOptions extends BaseConfigObject implements
    DSOInstrumentationLoggingOptions {

  public StandardDSOInstrumentationLoggingOptions(ConfigContext context) {
    super(context);
    this.context.ensureRepositoryProvides(DsoClientData.class);
  }

  public boolean logClass() {
    DsoClientData dsoClientData = (DsoClientData) this.context.bean();
    InstrumentationLogging logging = dsoClientData.getDebugging().getInstrumentationLogging();
    return logging.getClass1();
  }

  public boolean logLocks() {
    DsoClientData dsoClientData = (DsoClientData) this.context.bean();
    InstrumentationLogging logging = dsoClientData.getDebugging().getInstrumentationLogging();
    return logging.getLocks();
  }

  public boolean logTransientRoot() {
    DsoClientData dsoClientData = (DsoClientData) this.context.bean();
    InstrumentationLogging logging = dsoClientData.getDebugging().getInstrumentationLogging();
    return logging.getTransientRoot();
  }

  public boolean logRoots() {
    DsoClientData dsoClientData = (DsoClientData) this.context.bean();
    InstrumentationLogging logging = dsoClientData.getDebugging().getInstrumentationLogging();
    return logging.getRoots();
  }

  public boolean logDistributedMethods() {
    DsoClientData dsoClientData = (DsoClientData) this.context.bean();
    InstrumentationLogging logging = dsoClientData.getDebugging().getInstrumentationLogging();
    return logging.getDistributedMethods();
  }

  //used STRICTLY for test
  public void setLogDistributedMethods(boolean val){
    DsoClientData dsoClientData = (DsoClientData) this.context.bean();
    InstrumentationLogging logging = dsoClientData.getDebugging().getInstrumentationLogging();
    logging.setDistributedMethods(val);
  }
}
