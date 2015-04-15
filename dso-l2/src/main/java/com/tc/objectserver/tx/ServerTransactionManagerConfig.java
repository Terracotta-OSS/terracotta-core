/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
