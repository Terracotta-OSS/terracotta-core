/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

enum LookupState implements LookupStateTransition {

  UNINITALIZED {

    @Override
    public LookupState makeLookupRequest() {
      return LOOKUP_REQUEST;
    }

    @Override
    public LookupState makePrefetchRequest() {
      return PREFETCH_REQUEST;
    }
  },

  LOOKUP_REQUEST {

    @Override
    public LookupState makeMissingObject() {
      return MISSING_OBJECT_ID;
    }

    @Override
    public LookupState makePending() {
      return PENDING_LOOKUP;
    }

  },

  PREFETCH_REQUEST {

    @Override
    public boolean isPrefetch() {
      return true;
    }

    @Override
    public LookupState makeLookupRequest() {
      return LOOKUP_REQUEST;
    }

    @Override
    public LookupState makePending() {
      return PENDING_PREFETCH;
    }
  },

  PENDING_LOOKUP {

    @Override
    public boolean isPending() {
      return true;
    }

    @Override
    public LookupState makeUnPending() {
      return LOOKUP_REQUEST;
    }

    @Override
    public LookupState makeMissingObject() {
      return MISSING_OBJECT_ID;
    }
  },

  PENDING_PREFETCH {

    @Override
    public boolean isPrefetch() {
      return true;
    }

    @Override
    public LookupState makeLookupRequest() {
      return PENDING_LOOKUP;
    }

    @Override
    public boolean isPending() {
      return true;
    }

    @Override
    public LookupState makeUnPending() {
      return PREFETCH_REQUEST;
    }
  },

  MISSING_OBJECT_ID {
    @Override
    public boolean isMissing() {
      return true;
    }
  };

  public LookupState makeLookupRequest() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + LOOKUP_REQUEST);
  }

  public LookupState makeMissingObject() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + MISSING_OBJECT_ID);
  }

  public LookupState makePrefetchRequest() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + PREFETCH_REQUEST);
  }

  public LookupState makePending() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + PENDING_LOOKUP + " or "
                                    + PENDING_PREFETCH);
  }

  public LookupState makeUnPending() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + LOOKUP_REQUEST + " or "
                                    + PREFETCH_REQUEST);
  }

  public boolean isPrefetch() {
    return false;
  }

  public boolean isMissing() {
    return false;
  }

  public boolean isPending() {
    return false;
  }
}