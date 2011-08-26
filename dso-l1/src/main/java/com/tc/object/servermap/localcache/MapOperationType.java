/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

public enum MapOperationType {

  GET() {

    @Override
    public boolean isMutateOperation() {
      return false;
    }

    @Override
    public boolean isRemoveOperation() {
      return false;
    }

  },
  PUT() {

    @Override
    public boolean isMutateOperation() {
      return true;
    }

    @Override
    public boolean isRemoveOperation() {
      return false;
    }

  },
  REMOVE() {

    @Override
    public boolean isMutateOperation() {
      return true;
    }

    @Override
    public boolean isRemoveOperation() {
      return true;
    }

  };

  public abstract boolean isMutateOperation();

  public abstract boolean isRemoveOperation();
}
