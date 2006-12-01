/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.logging;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

import java.util.HashMap;
import java.util.Map;

public class RuntimeOutputOptionsImpl implements RuntimeOutputOptions {

  private static final String TO_STRING                 = "toString";
  private static final String AUTOLOCK_INSTANCE_DETAILS = "autoLockDetails";
  private static final String FULL_STACK                = "fullStack";
  private static final String CALLER                    = "caller";
  private static final String FIND_NEEDED_INCLUDES      = "findNeededIncludes";

  private static final Map    DEFAULTS                  = new HashMap();

  static {
    // set defaults here (otherwise default is false)
    DEFAULTS.put(FIND_NEEDED_INCLUDES, Boolean.TRUE);
  }

  private final Options       opts;

  public RuntimeOutputOptionsImpl(String input) {
    this(input, CustomerLogging.getDSOGenericLogger());
  }

  public RuntimeOutputOptionsImpl(String input, TCLogger logger) {
    String[] keys = Keys.getKeys(getClass());
    this.opts = new Options(input, keys, logger, DEFAULTS);
  }

  public boolean includeFullStack() {
    return opts.getOption(FULL_STACK);
  }

  public boolean includeToString() {
    return opts.getOption(TO_STRING);
  }

  public boolean includeAutolockInstanceDetails() {
    return opts.getOption(AUTOLOCK_INSTANCE_DETAILS);
  }

  public boolean includeCaller() {
    return opts.getOption(CALLER);
  }

  public boolean findNeededIncludes() {
    return opts.getOption(FIND_NEEDED_INCLUDES);
  }

}
