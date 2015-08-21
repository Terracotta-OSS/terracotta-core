/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.entity;

import com.tc.async.api.AbstractEventHandler;

public class RequestProcessorHandler extends AbstractEventHandler<Runnable> {

  @Override
  public void handleEvent(Runnable context) {
    context.run();
  }

}
