/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
