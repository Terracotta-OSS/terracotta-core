/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

public enum RemoveType {

  NORMAL() {

    @Override
    public boolean decrementSizeOnRemove() {
      return true;
    }

  },
  NO_SIZE_DECREMENT() {
    @Override
    public boolean decrementSizeOnRemove() {
      return false;
    }
  };

  public abstract boolean decrementSizeOnRemove();
}
