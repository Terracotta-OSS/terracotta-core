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
