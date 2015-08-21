/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

public class LowWaterMarkCallbackContext {
  private final Runnable callback;

  public LowWaterMarkCallbackContext(Runnable callback) {
    this.callback = callback;
  }

  public void run() {
    callback.run();
  }
}
