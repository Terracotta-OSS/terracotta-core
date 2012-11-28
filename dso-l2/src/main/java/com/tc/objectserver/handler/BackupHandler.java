package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;

import java.util.concurrent.Callable;

/**
 * @author tim
 */
public class BackupHandler extends AbstractEventHandler {
  @Override
  public void handleEvent(final EventContext context) throws EventHandlerException {
    if (context instanceof Callable) {
      try {
        ((Callable<Void>)context).call();
      } catch (Exception e) {
        throw new EventHandlerException(e);
      }
    } else {
      throw new EventHandlerException("Unknown event context type " + context.getClass());
    }
  }
}
