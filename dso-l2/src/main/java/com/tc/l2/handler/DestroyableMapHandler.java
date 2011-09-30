/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.objectserver.persistence.db.TCDestroyable;

public class DestroyableMapHandler extends AbstractEventHandler implements EventHandler {
  @Override
  public void handleEvent(EventContext context) {
    DestroyableMapContext destroyableMapContext = (DestroyableMapContext) context;
    destroyableMapContext.getDestroyable().destroy();
  }

  public static class DestroyableMapContext implements EventContext {
    private final TCDestroyable destroyable;

    public DestroyableMapContext(TCDestroyable destroyable) {
      this.destroyable = destroyable;
    }

    public TCDestroyable getDestroyable() {
      return destroyable;
    }
  }
}
