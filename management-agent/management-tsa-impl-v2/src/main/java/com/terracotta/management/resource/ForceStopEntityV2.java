/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.io.Serializable;

public class ForceStopEntityV2 implements Serializable {
  private boolean forceStop;

  public boolean isForceStop() {
    return forceStop;
  }

  public void setForceStop(boolean forceStop) {
    this.forceStop = forceStop;
  }


}
