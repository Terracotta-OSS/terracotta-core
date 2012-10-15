/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.objectserver.api.Destroyable;

public class DestroyableMapHandler extends AbstractEventHandler implements EventHandler {
  @Override
  public void handleEvent(EventContext context) {
    DestroyableMapContext destroyableMapContext = (DestroyableMapContext) context;
    destroyableMapContext.getDestroyable().destroy();
  }

  public static class DestroyableMapContext implements EventContext {
    private final Destroyable destroyable;

    public DestroyableMapContext(Destroyable destroyable) {
      this.destroyable = destroyable;
    }

    public Destroyable getDestroyable() {
      return destroyable;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final DestroyableMapContext that = (DestroyableMapContext)o;

      // Do a reference equality instead of equals() here. It won't make sense to have two instances of the same
      // object to destroy.
      return that.destroyable == destroyable;
    }

    @Override
    public int hashCode() {
      return destroyable != null ? System.identityHashCode(destroyable) : 0;
    }
  }
}
