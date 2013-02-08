/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;

public final class ServerTransactionManagerConfig {

  private final boolean loggingEnabled;
  private final boolean verboseLogging;
  private final boolean printStats;
  private final boolean printCommits;
  private final boolean printBroadcastStats;

  public ServerTransactionManagerConfig(TCProperties tcproperties) {
    this.loggingEnabled = tcproperties.getBoolean(TCPropertiesConsts.L2_TRANSACTIONMANAGER_LOGGING_ENABLED);
    this.verboseLogging = tcproperties.getBoolean(TCPropertiesConsts.L2_TRANSACTIONMANAGER_LOGGING_VERBOSE);
    this.printStats = tcproperties.getBoolean(TCPropertiesConsts.L2_TRANSACTIONMANAGER_LOGGING_PRINTSTATS);
    this.printCommits = tcproperties.getBoolean(TCPropertiesConsts.L2_TRANSACTIONMANAGER_LOGGING_PRINTCOMMITS);
    this.printBroadcastStats = tcproperties.getBoolean(TCPropertiesConsts.L2_TRANSACTIONMANAGER_LOGGING_PRINT_BROADCAST_STATS);
  }

  // Used in tests
  public ServerTransactionManagerConfig() {
    this.loggingEnabled = false;
    this.verboseLogging = false;
    this.printStats = false;
    this.printCommits = false;
    this.printBroadcastStats = false;
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

  public boolean isPrintCommitsEnabled() {
    return printCommits;
  }

  public boolean isPrintBroadcastStatsEnabled() {
    return printBroadcastStats;
  }
}
