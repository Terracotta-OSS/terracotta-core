/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;

public class LowWaterMarkCallbackContext implements EventContext {
  private final Runnable callback;

  public LowWaterMarkCallbackContext(Runnable callback) {
    this.callback = callback;
  }

  public void run() {
    callback.run();
  }
}
