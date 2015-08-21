/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

public interface LookupStateTransition {

  public LookupState makeLookupRequest();

  public LookupState makeMissingObject();

  public LookupState makePending();

  public LookupState makeUnPending();

  public boolean isMissing();

  public boolean isPending();

}
