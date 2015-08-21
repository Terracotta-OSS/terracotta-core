/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.objectserver.context.LowWaterMarkCallbackContext;

public class LowWaterMarkCallbackHandler extends AbstractEventHandler<LowWaterMarkCallbackContext> {
  @Override
  public void handleEvent(LowWaterMarkCallbackContext context) {
    context.run();
  }
}
