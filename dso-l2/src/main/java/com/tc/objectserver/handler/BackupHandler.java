package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;

import java.util.concurrent.Callable;

/**
 * @author tim
 */
public class BackupHandler extends AbstractEventHandler<Callable<?>> {
  @Override
  public void handleEvent(Callable<?> context) throws EventHandlerException {
    try {
      context.call();
    } catch (Exception e) {
      throw new EventHandlerException(e);
    }
  }
}
