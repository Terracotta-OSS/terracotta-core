/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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