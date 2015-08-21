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

  MISSING_OBJECT_ID {
    @Override
    public boolean isMissing() {
      return true;
    }

    @Override
    public LookupState makeMissingObject() {
      return MISSING_OBJECT_ID;
    }
    
    
  };

  @Override
  public LookupState makeLookupRequest() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + LOOKUP_REQUEST);
  }

  @Override
  public LookupState makeMissingObject() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + MISSING_OBJECT_ID);
  }

  @Override
  public LookupState makePending() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + PENDING_LOOKUP);
  }

  @Override
  public LookupState makeUnPending() {
    throw new IllegalStateException("Current State : " + toString() + ". Can't go to " + LOOKUP_REQUEST);
  }



  @Override
  public boolean isMissing() {
    return false;
  }

  @Override
  public boolean isPending() {
    return false;
  }
}