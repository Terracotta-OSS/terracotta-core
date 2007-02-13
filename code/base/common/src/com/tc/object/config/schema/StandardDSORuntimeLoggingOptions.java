/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.BaseNewConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.terracottatech.config.DsoClientData;

/**
 * The standard implementation of {@link DSORuntimeLoggingOptions}.
 */
public class StandardDSORuntimeLoggingOptions extends BaseNewConfigObject implements DSORuntimeLoggingOptions {

  private final BooleanConfigItem logLockDebug;
  private final BooleanConfigItem logPartialInstrumentation;
  private final BooleanConfigItem logFieldChangeDebug;
  private final BooleanConfigItem logNonPortableWarning;
  private final BooleanConfigItem logWaitNotifyDebug;
  private final BooleanConfigItem logDistributedMethodDebug;
  private final BooleanConfigItem logNewObjectDebug;

  public StandardDSORuntimeLoggingOptions(ConfigContext context) {
    super(context);
    this.context.ensureRepositoryProvides(DsoClientData.class);

    this.logLockDebug = this.context.booleanItem("debugging/runtime-logging/lock-debug");
    this.logPartialInstrumentation = this.context.booleanItem("debugging/runtime-logging/partial-instrumentation");
    this.logFieldChangeDebug = this.context.booleanItem("debugging/runtime-logging/field-change-debug");
    this.logNonPortableWarning = this.context.booleanItem("debugging/runtime-logging/non-portable-warning");
    this.logWaitNotifyDebug = this.context.booleanItem("debugging/runtime-logging/wait-notify-debug");
    this.logDistributedMethodDebug = this.context.booleanItem("debugging/runtime-logging/distributed-method-debug");
    this.logNewObjectDebug = this.context.booleanItem("debugging/runtime-logging/new-object-debug");
  }

  public BooleanConfigItem logLockDebug() {
    return this.logLockDebug;
  }

  public BooleanConfigItem logPartialInstrumentation() {
    return this.logPartialInstrumentation;
  }

  public BooleanConfigItem logFieldChangeDebug() {
    return this.logFieldChangeDebug;
  }

  public BooleanConfigItem logNonPortableWarning() {
    return this.logNonPortableWarning;
  }

  public BooleanConfigItem logWaitNotifyDebug() {
    return this.logWaitNotifyDebug;
  }

  public BooleanConfigItem logDistributedMethodDebug() {
    return this.logDistributedMethodDebug;
  }

  public BooleanConfigItem logNewObjectDebug() {
    return this.logNewObjectDebug;
  }

}
