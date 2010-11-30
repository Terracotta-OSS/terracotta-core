/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.BaseConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.terracottatech.config.DsoClientData;
import com.terracottatech.config.RuntimeLogging;

/**
 * The standard implementation of {@link DSORuntimeLoggingOptions}.
 */
public class StandardDSORuntimeLoggingOptions extends BaseConfigObject implements DSORuntimeLoggingOptions {

  private final boolean logLockDebug;
  private final boolean logFieldChangeDebug;
  private final boolean logWaitNotifyDebug;
  private final boolean logDistributedMethodDebug;
  private final boolean logNewObjectDebug;
  private final boolean logNonPortableDump;
  private final boolean logNamedLoaderDebug;

  public StandardDSORuntimeLoggingOptions(ConfigContext context) {
    super(context);
    this.context.ensureRepositoryProvides(DsoClientData.class);

    DsoClientData dsoClientData = (DsoClientData) this.context.bean();
    RuntimeLogging runtimeLogging = dsoClientData.getDebugging().getRuntimeLogging();
    this.logLockDebug = runtimeLogging.getLockDebug();
    this.logFieldChangeDebug = runtimeLogging.getFieldChangeDebug();
    this.logWaitNotifyDebug = runtimeLogging.getWaitNotifyDebug();
    this.logDistributedMethodDebug = runtimeLogging.getDistributedMethodDebug();
    this.logNewObjectDebug = runtimeLogging.getNewObjectDebug();
    this.logNonPortableDump = runtimeLogging.getNonPortableDump();
    this.logNamedLoaderDebug = runtimeLogging.getNamedLoaderDebug();
  }

  public boolean logLockDebug() {
    return this.logLockDebug;
  }

  public boolean logFieldChangeDebug() {
    return this.logFieldChangeDebug;
  }

  public boolean logWaitNotifyDebug() {
    return this.logWaitNotifyDebug;
  }

  public boolean logDistributedMethodDebug() {
    return this.logDistributedMethodDebug;
  }

  public boolean logNewObjectDebug() {
    return this.logNewObjectDebug;
  }

  public boolean logNonPortableDump() {
    return this.logNonPortableDump;
  }

  public boolean logNamedLoaderDebug() {
    return this.logNamedLoaderDebug;
  }

}
