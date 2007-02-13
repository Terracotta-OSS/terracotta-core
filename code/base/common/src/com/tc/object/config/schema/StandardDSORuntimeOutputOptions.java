/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.BaseNewConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.terracottatech.config.DsoClientData;

/**
 * The standard implementation of {@link DSORuntimeOutputOptions}.
 */
public class StandardDSORuntimeOutputOptions extends BaseNewConfigObject implements DSORuntimeOutputOptions {

  private final BooleanConfigItem doAutoLockDetails;
  private final BooleanConfigItem doCaller;
  private final BooleanConfigItem doFullStack;
  private final BooleanConfigItem doFindNeededIncludes;

  public StandardDSORuntimeOutputOptions(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(DsoClientData.class);

    this.doAutoLockDetails = this.context.booleanItem("debugging/runtime-output-options/auto-lock-details");
    this.doCaller = this.context.booleanItem("debugging/runtime-output-options/caller");
    this.doFullStack = this.context.booleanItem("debugging/runtime-output-options/full-stack");
    this.doFindNeededIncludes = this.context.booleanItem("debugging/runtime-output-options/find-needed-includes");
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

  public BooleanConfigItem doFindNeededIncludes() {
    return this.doFindNeededIncludes;
  }

}
