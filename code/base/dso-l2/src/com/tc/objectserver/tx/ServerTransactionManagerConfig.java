/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.properties.TCProperties;

public final class ServerTransactionManagerConfig {

  private final boolean loggingEnabled;
  private final boolean verboseLogging;
  private final boolean printStats;

  public ServerTransactionManagerConfig(TCProperties tcproperties) {
    this.loggingEnabled = tcproperties.getBoolean("logging.enabled");
    this.verboseLogging = tcproperties.getBoolean("logging.verbose");
    this.printStats = tcproperties.getBoolean("logging.printStats");
  }

  // Used in tests
  public ServerTransactionManagerConfig() {
    this.loggingEnabled = false;
    this.verboseLogging = false;
    this.printStats = false;
  }

  public boolean isLoggingEnabled() {
    return loggingEnabled;
  }

  public boolean isPrintStatsEnabled() {
    return printStats;
  }

  public boolean isVerboseLogging() {
    return verboseLogging;
  }

}
