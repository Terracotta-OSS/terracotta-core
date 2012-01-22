/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

enum CachedItemState {
  UNACKED_NOT_ACCESSED {
    @Override
    public CachedItemState accessed() {
      return UNACKED_ACCESSED;
    }

    @Override
    public CachedItemState clearAccessed() {
      return this;
    }

    @Override
    public boolean isAccessed() {
      // returning true as its still in UNACKED state.
      return true;
    }

    @Override
    public CachedItemState acknowledged() {
      return ACKED_NOT_ACCESSED;
    }
  },
  UNACKED_ACCESSED {
    @Override
    public CachedItemState clearAccessed() {
      return UNACKED_NOT_ACCESSED;
    }

    @Override
    public CachedItemState accessed() {
      return this;
    }

    @Override
    public boolean isAccessed() {
      return true;
    }

    @Override
    public CachedItemState acknowledged() {
      return ACKED_ACCESSED;
    }
  },
  ACKED_NOT_ACCESSED {
    @Override
    public CachedItemState accessed() {
      return ACKED_ACCESSED;
    }

    @Override
    public boolean isAccessed() {
      return false;
    }

    @Override
    public CachedItemState clearAccessed() {
      return this;
    }

  },
  ACKED_ACCESSED {
    @Override
    public CachedItemState accessed() {
      return this;
    }

    @Override
    public boolean isAccessed() {
      return true;
    }

    @Override
    public CachedItemState clearAccessed() {
      return ACKED_NOT_ACCESSED;
    }
  },
  REMOVE_ON_TXN_COMPLETE {
    @Override
    public CachedItemState accessed() {
      return this;
    }

    @Override
    public boolean isAccessed() {
      return true;
    }

    @Override
    public CachedItemState clearAccessed() {
      return this;
    }
  };

  public CachedItemState accessed() {
    throw new UnsupportedOperationException();
  }

  public boolean isAccessed() {
    throw new UnsupportedOperationException();
  }

  public CachedItemState clearAccessed() {
    throw new UnsupportedOperationException();
  }

  public CachedItemState acknowledged() {
    throw new UnsupportedOperationException();
  }

}
