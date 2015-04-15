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
package com.tc.test.config.model;

import java.io.Serializable;

/**
 * Pause Configuration for a Process to simluate Long GC Pauses. Use by TestFramwork. Especially by PauseManager.
 */
public final class PauseConfig implements Serializable {

  private final long pauseTime;
  private final long pauseInterval;
  private long       initialDelay = 5000; // in millis
  private int        maxPauses    = 30;

  public PauseConfig(long pauseTime, long pauseInterval) {
    this.pauseTime = pauseTime;
    this.pauseInterval = pauseInterval;
    if (pauseInterval <= pauseTime) {
      throw new IllegalArgumentException(  "pauseInterval "
          + pauseInterval
          + "should be greater than pauseTime "
          + pauseTime);
    }
  }

  public PauseConfig(long pauseTime) {
    super();
    this.pauseTime = pauseTime;
    this.pauseInterval = 0;
  }

  public long getPauseTime() {
    return pauseTime;
  }

  public long getPauseInterval() {
    return pauseInterval;
  }

  public int getMaxPauses() {
    return maxPauses;
  }

  public void setMaxPauses(int maxPauses) {
    this.maxPauses = maxPauses;
  }

  public long getInitialDelay() {
    return initialDelay;
  }

  public void setInitialDelay(long initialDelay) {
    this.initialDelay = initialDelay;
  }


}
