package com.tc.objectserver.tx;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;

/**
 * @author tim
 */
public enum BroadcastDurabilityLevel {
  NONE(false, false), RELAYED(true, false), DISK(true, true);

  private final boolean waitForRelay;
  private final boolean waitForCommit;

  BroadcastDurabilityLevel(final boolean waitForRelay, final boolean waitForCommit) {
    this.waitForRelay = waitForRelay;
    this.waitForCommit = waitForCommit;
  }

  public boolean isWaitForRelay() {
    return waitForRelay;
  }

  public boolean isWaitForCommit() {
    return waitForCommit;
  }

  public static BroadcastDurabilityLevel getFromProperties(TCProperties tcProperties) {
    String v = tcProperties.getProperty(TCPropertiesConsts.L2_TRANSACTIONMANAGER_BROADCAST_DURABILITY_LEVEL, true);
    return v == null ? RELAYED : valueOf(v.toUpperCase());
  }
}
