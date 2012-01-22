/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.Config;

/**
 * Represents the runtime-output options for DSO.
 */
public interface DSORuntimeOutputOptions extends Config {

  boolean doAutoLockDetails();

  boolean doCaller();

  boolean doFullStack();

}
