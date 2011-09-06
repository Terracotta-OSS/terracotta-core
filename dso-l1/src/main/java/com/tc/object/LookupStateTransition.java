/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

public interface LookupStateTransition {

  public LookupState makeLookupRequest();

  public LookupState makeMissingObject();

  public LookupState makePrefetchRequest();

  public LookupState makePending();

  public LookupState makeUnPending();

  public boolean isPrefetch();

  public boolean isMissing();

  public boolean isPending();

}
