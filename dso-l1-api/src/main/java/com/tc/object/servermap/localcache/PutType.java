/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

public enum PutType {

  NORMAL() {

    @Override
    public boolean incrementSizeOnPut() {
      return true;
    }

    @Override
    public boolean isPinned() {
      return false;
    }

  },
  PINNED() {
    @Override
    public boolean incrementSizeOnPut() {
      return true;
    }

    @Override
    public boolean isPinned() {
      return true;
    }
  },
  PINNED_NO_SIZE_INCREMENT() {
    @Override
    public boolean incrementSizeOnPut() {
      return false;
    }

    @Override
    public boolean isPinned() {
      return true;
    }
  };

  public abstract boolean incrementSizeOnPut();

  public abstract boolean isPinned();

}
