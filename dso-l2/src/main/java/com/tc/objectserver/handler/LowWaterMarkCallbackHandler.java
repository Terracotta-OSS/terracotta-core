/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.objectserver.context.LowWaterMarkCallbackContext;

public class LowWaterMarkCallbackHandler extends AbstractEventHandler {
  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof LowWaterMarkCallbackContext) {
      ((LowWaterMarkCallbackContext) context).run();
    }
  }
}
